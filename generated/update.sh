#!/bin/bash

function init() {

    module=$1; shift
    src=$1; shift

    if [ -e $module/src ]; then
        rm -rf $module/src/main
    fi

    mkdir -p $module/src/main

    cp -rf $src/src/main/java $src/src/main/resources $module/src/main

}

function optionals() {
	project=$1; shift
	./gradlew spring-boot-project:$project:dependencies --configuration optional | grep '^+---' | sed -e 's/+--- //' -e 's/ ->.*//' | egrep -v '^project' | awk -f <(cat - <<EOF
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
    artifactId=$1; shift;
    version=$1; shift;
	opts=$1; shift
    if ! [ -e $pom ]; then
        cat > $pom <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<artifactId>${artifactId}</artifactId>
	<version>${version}</version>

	<parent>
    	<groupId>org.springframework.experimental</groupId>
		<artifactId>spring-init-generated</artifactId>
		<version>0.0.1.BUILD-SNAPSHOT</version>
	</parent>

	<dependencies>
	</dependencies>

	<properties>
		<java.version>1.8</java.version>
		<slim.version>0.0.1.BUILD-SNAPSHOT</slim.version>
	</properties>

	<build>
		<plugins>
			<plugin>
				<artifactId>maven-jar-plugin</artifactId>
				<configuration>
					<classifier>func</classifier>
					<includes>
						<include>**/*Initializer.class</include>
					</includes>
				</configuration>
			</plugin>
			<plugin>
				<artifactId>maven-source-plugin</artifactId>
				<configuration>
					<classifier>func-sources</classifier>
					<includes>
						<include>**/*Initializer.java</include>
					</includes>
				</configuration>
				<executions>
					<execution>
						<id>attach-sources</id>
						<goals>
							<goal>jar</goal>
						</goals>
						<phase>package</phase>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>

</project>
EOF
    fi

    # Delete the dependencies
    sed -i.bak -e '\!<dependencies!,\!</dependencies!{/dependencies>/!d;}' $pom && rm $pom.bak

    # Build them back up
    tmpfile=.pom.xml
    sed '/<\/dependencies/,$ d' $pom > $tmpfile
    sed -e '1,/<dependencies/ d;/<\/dependencies/,$ d' -e '/<dependency>/{:a;N;/<\/dependency>/!ba};/<scope>test/d' $src >> $tmpfile
	if ! [ -z ${opts} ] && [ -f ${opts} ]; then cat $opts >> $tmpfile; fi
    cat >> $tmpfile <<EOF
		<dependency>
			<groupId>com.google.code.findbugs</groupId>
			<artifactId>jsr305</artifactId>
			<version>3.0.2</version>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-init-library</artifactId>
			<version>\${slim.version}</version>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-init-core</artifactId>
			<version>\${slim.version}</version>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>org.springframework.experimental</groupId>
			<artifactId>spring-init-processor</artifactId>
			<version>\${slim.version}</version>
			<scope>provided</scope>
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

(cd $cache; git fetch --tags && git checkout v2.3.0.RELEASE)
(cd $cache; ./gradlew publishMavenPublicationToMavenLocal -x test)

src=$cache/spring-boot-project/spring-boot-autoconfigure
tgt=`dirname $0`/autoconfigure
init $tgt $src
(cd $cache; mkdir -p build && optionals spring-boot-autoconfigure > build/opts)
generate $src/build/publications/maven/pom-default.xml $tgt/pom.xml spring-boot-autoconfigure 2.3.0.BUILD-SNAPSHOT $cache/build/opts

src=$cache/spring-boot-project/spring-boot-actuator-autoconfigure
tgt=`dirname $0`/actuator
init $tgt $src
(cd $cache; mkdir -p build && optionals spring-boot-actuator-autoconfigure > build/opts)
generate $src/build/publications/maven/pom-default.xml $tgt/pom.xml spring-boot-actuator-autoconfigure 2.3.0.BUILD-SNAPSHOT $cache/build/opts

src=$cache/spring-boot-project/spring-boot-test-autoconfigure
tgt=`dirname $0`/test
init $tgt $src
(cd $cache; mkdir -p build && optionals spring-boot-test-autoconfigure > build/opts)
generate $src/build/publications/maven/pom-default.xml $tgt/pom.xml spring-boot-test-autoconfigure 2.3.0.BUILD-SNAPSHOT $cache/build/opts

cache=`dirname $0`/sources/spring-security
if ! [ -e $cache ]; then
    git clone https://github.com/spring-projects/spring-security $cache
fi

(cd $cache; git fetch --tags && git checkout 5.3.2.RELEASE)
(cd $cache/config; ../gradlew install -x test)

src=$cache/config
tgt=`dirname $0`/security
init $tgt $src
generate $src/build/poms/pom-default.xml $tgt/pom.xml spring-security-config 5.3.2.BUILD-SNAPSHOT


