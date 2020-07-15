#!/bin/bash

set -uex

DAPR_JAVA_SDK_VERSION=$1

mvn versions:set -DnewVersion=$DAPR_JAVA_SDK_VERSION
mvn versions:commit

if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i bak "s/<dapr.sdk.version>.*<\/dapr.sdk.version>/<dapr.sdk.version>${DAPR_JAVA_SDK_VERSION}<\/dapr.sdk.version>/g" sdk-tests/pom.xml
  rm sdk-tests/pom.xmlbak
else
  sed -i "s/<dapr.sdk.version>.*<\/dapr.sdk.version>/<dapr.sdk.version>${DAPR_JAVA_SDK_VERSION}<\/dapr.sdk.version>/g" sdk-tests/pom.xml
fi
