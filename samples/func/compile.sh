#!/usr/bin/env bash

ARTIFACT=func
MAINCLASS=app.main.SampleApplication
VERSION=0.0.1-SNAPSHOT

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

rm -rf target
mkdir -p target/native-image

echo "Packaging $ARTIFACT with Maven"
mvn -ntp package > target/native-image/output.txt
mvn -ntp dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/native-image/lib/ >> target/native-image/output.txt

if [ "$1" == "-d" ]; then
  mvn -ntp dependency:copy-dependencies -DincludeScope=runtime -Dclassifier=sources -DoutputDirectory=target/native-image/lib/ >> target/native-image/output.txt
  DEBUG_FLAGS="-H:GenerateDebugInfo=1 -H:DebugInfoSourceSearchPath=../../src/main/java"
  shift
fi

find target/native-image/lib -type f -not -name \*.jar -exec rm {} \;

cp -rf target/classes target/native-image
cd target/native-image
LIBPATH=`find lib -name \*.jar | egrep -v sources | tr '\n' ':'`
CP=classes:$LIBPATH

GRAALVM_VERSION=`native-image --version`
echo "Compiling $ARTIFACT with $GRAALVM_VERSION"
{ time native-image \
  --verbose $DEBUG_FLAGS \
  -H:Name=$ARTIFACT \
  -H:+UseServiceLoaderFeature \
  -Dspring.native.mode=functional \
  -Dspring.xml.ignore=true \
  -Dspring.spel.ignore=true \
  -Dspring.native.remove-yaml-support=true \
  -Dspring.native.remove-jmx-support=true \
  --initialize-at-build-time=org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfigurationInitializer \
  --initialize-at-build-time=org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryAutoConfigurationInitializer \
  --initialize-at-build-time=org.springframework.boot.autoconfigure.web.reactive.ReactiveWebServerFactoryConfiguration_EmbeddedNettyInitializer \
  --initialize-at-build-time=org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration_NettyWebServerFactoryCustomizerConfigurationInitializer \
  --initialize-at-build-time=org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfigurationInitializer \
  --initialize-at-build-time=org.springframework.boot.autoconfigure.web.reactive.RouterFunctionAutoConfigurationInitializer \
  --initialize-at-build-time=org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfigurationInitializer \
  --initialize-at-build-time=org.springframework.boot.autoconfigure.web.reactive.HttpHandlerAutoConfigurationInitializer \
  -H:+PrintAnalysisCallTree \
  -cp "${CP}" $MAINCLASS >> output.txt ; } 2>> output.txt

if [[ -f $ARTIFACT ]]
then
  printf "${GREEN}SUCCESS${NC}\n"
  mv ./$ARTIFACT ..
  exit 0
else
  cat output.txt
  printf "${RED}FAILURE${NC}: an error occurred when compiling the native-image.\n"
  exit 1
fi
