#!/bin/bash

BOOT_FUNC_VERSION=0.2.1-SNAPSHOT
BOOT_LABEL=2.4.0-M3
INIT_VERSION=0.2.1-SNAPSHOT

function init() {

    module=$1; shift
    src=$1; shift

    if [ -e $module/src ]; then
        rm -rf $module/src/main
    fi

    mkdir -p $module/src/main

}

function optionals() {
	project=$1; shift
	./gradlew spring-boot-project:$project:dependencies --configuration optional | grep '^[^-]---' | sed -e 's/.--- //' -e 's/ ->.*//' | egrep -v '^project' | awk -f <(cat - <<EOF
BEGIN { FS=":" }
{
  print "        <dependency>"
  print "          <groupId>" \$1 "</groupId>"
  print "          <artifactId>" \$2 "</artifactId>"
  if (NF>2) {
    print "          <version>" \$3 "</version>"
  }
  print "          <optional>true</optional>"
  print "        </dependency>"
}
EOF
)		
}

function generate() {
    src=$1; shift
    pom=$1; shift
    basePackage=$1; shift;
    groupId=$1; shift;
    artifactId=$1; shift;
    version=$1; shift;
	func_version=$1; shift
	opts=$1; shift
	echo Generating: ${artifactId}:${func_version}
    if ! [ -e $pom ]; then
        cat > $pom <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<artifactId>${artifactId}-func</artifactId>
	<version>${func_version}</version>

	<parent>
    	<groupId>org.springframework.experimental</groupId>
		<artifactId>spring-init-generated</artifactId>
		<version>${INIT_VERSION}</version>
	</parent>

	<dependencies>
	</dependencies>

	<properties>
		<java.version>1.8</java.version>
		<slim.version>${INIT_VERSION}</slim.version>
	</properties>

	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.experimental</groupId>
				<artifactId>spring-init-maven-plugin</artifactId>
				<executions>
					<execution>
						<id>sources</id>
						<phase>process-sources</phase>
						<goals>
							<goal>generate</goal>
						</goals>
						<inherited>false</inherited>
						<configuration>
							<basePackage>${basePackage}</basePackage>
							<nativeImageDirectory>src/main/resources/META-INF/${project.groupId}/${project.artifactId}</nativeImageDirectory>
						</configuration>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
EOF
	else
		tmpfile=.pom.xml
		sed '/^\t<version/,$ d' $pom > $tmpfile
    cat >> $tmpfile <<EOF
	<version>${func_version}</version>
EOF
		sed '1,/^\t<version/ d' $pom >> $tmpfile
		mv $tmpfile $pom
    fi

    # Delete the dependencies
    sed -i.bak -e '\!<dependencies!,\!</dependencies!{/dependencies>/!d;}' $pom && rm $pom.bak

    # Build them back up
    tmpfile=.pom.xml
    sed '/<\/dependencies/,$ d' $pom > $tmpfile
    sed -e '1,/<dependencies/ d;/<\/dependencies/,/<dependencies/ d;/<\/dependencies/,$ d' -e '/<dependency>/{:a;N;/<\/dependency>/!ba};/<scope>test/d'  $src | egrep -v '<scope>' >> $tmpfile
	if ! [ -z ${opts} ] && [ -f ${opts} ]; then cat $opts >> $tmpfile; fi
	if [ "${artifactId}" == "spring-boot-actuator-autoconfigure" ]; then
    cat >> $tmpfile <<EOF
		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-boot-autoconfigure-func</artifactId>
			<version>\${slim.version}</version>
			<scope>provided</scope>
		</dependency>
EOF
	fi
    cat >> $tmpfile <<EOF
		<dependency>
			<groupId>${groupId}</groupId>
			<artifactId>${artifactId}</artifactId>
			<version>${version}</version>
			<scope>provided</scope>
		</dependency>
		<dependency>
			<groupId>com.google.code.findbugs</groupId>
			<artifactId>jsr305</artifactId>
			<version>3.0.2</version>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-init-core</artifactId>
			<version>\${slim.version}</version>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>
	</dependencies>
EOF
    sed '1,/<\/dependencies>/ d' $pom >> $tmpfile
    mv $tmpfile $pom

}

cache=`dirname $0`/sources/spring-boot
if ! [ -e $cache ]; then
    git clone https://github.com/spring-projects/spring-boot $cache
fi

# If ref is a branch not a tag, remember to reset to origin, not just checkout
if echo $BOOT_LABEL | grep -q SNAPSHOT; then
	(cd $cache; git fetch --tags && git checkout master && git reset --hard origin/master)
else
	(cd $cache; git fetch --tags && git checkout v$BOOT_LABEL)
fi
(cd $cache; ./gradlew publishMavenPublicationToMavenLocal -x test)

src=$cache/spring-boot-project/spring-boot-autoconfigure
tgt=`dirname $0`/autoconfigure
init $tgt $src
(cd $cache; mkdir -p build && optionals spring-boot-autoconfigure > build/opts-autoconfigure)
generate $src/build/publications/maven/pom-default.xml $tgt/pom.xml org.springframework.boot.autoconfigure  org.springframework.boot spring-boot-autoconfigure $BOOT_LABEL $BOOT_FUNC_VERSION $cache/build/opts-autoconfigure

src=$cache/spring-boot-project/spring-boot-actuator-autoconfigure
tgt=`dirname $0`/actuator
init $tgt $src
(cd $cache; mkdir -p build && optionals spring-boot-actuator-autoconfigure > build/opts-actuator-autoconfigure)
generate $src/build/publications/maven/pom-default.xml $tgt/pom.xml org.springframework.boot.actuate.autoconfigure org.springframework.boot spring-boot-actuator-autoconfigure $BOOT_LABEL $BOOT_FUNC_VERSION $cache/build/opts-actuator-autoconfigure

src=$cache/spring-boot-project/spring-boot-test-autoconfigure
tgt=`dirname $0`/test
init $tgt $src
(cd $cache; mkdir -p build && optionals spring-boot-test-autoconfigure > build/opts-test-autoconfigure)
generate $src/build/publications/maven/pom-default.xml $tgt/pom.xml org.springframework.boot.test org.springframework.boot spring-boot-test-autoconfigure $BOOT_LABEL $BOOT_FUNC_VERSION $cache/build/opts-test-autoconfigure
