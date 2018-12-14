#!/bin/bash

cache=`dirname $0`/sources/spring-boot
if ! [ -e $cache ]; then
    git clone https://github.com/spring-projects/spring-boot $cache
fi

(cd $cache; git checkout v2.1.1.RELEASE)

function init() {

    module=$1; shift
    src=$1; shift

    if [ -e $module/src ]; then
        rm -rf $module/src/main
    fi

    mkdir -p $module/src/main

    cp -rf $src/src/main/java $src/src/main/resources $module/src/main

}

function generate() {
    src=$1; shift
    pom=$1; shift
    artifactId=$1; shift;
    if ! [ -e $pom ]; then
        cat > $pom <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<groupId>spring-init-experiment</groupId>
	<artifactId>${artifactId}</artifactId>
	<version>2.1.1.BUILD-SNAPSHOT</version>

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>2.1.1.RELEASE</version>
		<relativePath/>
	</parent>

	<dependencies>
	</dependencies>

	<properties>
		<java.version>1.8</java.version>
		<slim.version>1.0-SNAPSHOT</slim.version>
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

	<repositories>
		<repository>
			<id>spring-libs-snapshot</id>
			<url>http://repo.spring.io/libs-snapshot</url>
			<snapshots>
				<enabled>true</enabled>
			</snapshots>
			<releases>
				<enabled>true</enabled>
			</releases>
		</repository>
		<repository>
			<id>spring-libs-milestone</id>
			<url>http://repo.spring.io/libs-milestone</url>
			<snapshots>
				<enabled>false</enabled>
			</snapshots>
			<releases>
				<enabled>true</enabled>
			</releases>
		</repository>
	</repositories>

	<pluginRepositories>
		<pluginRepository>
			<id>spring-libs-snapshot</id>
			<url>http://repo.spring.io/libs-snapshot</url>
			<snapshots>
				<enabled>true</enabled>
			</snapshots>
			<releases>
				<enabled>true</enabled>
			</releases>
		</pluginRepository>
		<pluginRepository>
			<id>spring-libs-milestone</id>
			<url>http://repo.spring.io/libs-milestone</url>
			<snapshots>
				<enabled>true</enabled>
			</snapshots>
			<releases>
				<enabled>true</enabled>
			</releases>
		</pluginRepository>
	</pluginRepositories>

</project>
EOF
    fi

    # Delete the dependencies
    sed -i.bak -e '\!<dependencies!,\!</dependencies!{/dependencies>/!d;}' $pom && rm $pom.bak

    # Build them back up
    tmpfile=.pom.xml
    sed '/<\/dependencies/,$ d' $pom > $tmpfile
    sed '1,/<dependencies/ d;/<!-- Test/,$ d' $src >> $tmpfile
    cat >> $tmpfile <<EOF
		<dependency>
			<groupId>com.google.code.findbugs</groupId>
			<artifactId>jsr305</artifactId>
			<version>3.0.2</version>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>spring-init-experiment</groupId>
			<artifactId>library</artifactId>
			<version>\${slim.version}</version>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>spring-init-experiment</groupId>
			<artifactId>slim</artifactId>
			<version>\${slim.version}</version>
			<optional>true</optional>
		</dependency>
		<dependency>
			<groupId>spring-init-experiment</groupId>
			<artifactId>processor</artifactId>
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

src=$cache/spring-boot-project/spring-boot-autoconfigure
tgt=`dirname $0`/autoconfigure
init $tgt $src
generate $src/pom.xml $tgt/pom.xml spring-boot-autoconfigure

src=$cache/spring-boot-project/spring-boot-actuator-autoconfigure
tgt=`dirname $0`/actuator
init $tgt $src
generate $src/pom.xml $tgt/pom.xml spring-boot-actuator-autoconfigure

src=$cache/spring-boot-project/spring-boot-test-autoconfigure
tgt=`dirname $0`/test
init $tgt $src
generate $src/pom.xml $tgt/pom.xml spring-boot-test-autoconfigure
