#!/bin/bash

set -uex

DAPR_JAVA_SDK_VERSION=$1

if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i bak "s/<version>.*<\/version>\$/<version>${DAPR_JAVA_SDK_VERSION}<\/version>/g" README.md
  sed -i bak "s/compile('io.dapr:\(.*\):.*')/compile('io.dapr:\\1:${DAPR_JAVA_SDK_VERSION}')/g" README.md
  rm README.mdbak
else
  sed -i "s/<version>.*<\/version>\$/<version>${DAPR_JAVA_SDK_VERSION}<\/version>/g" README.md
  sed -i "s/compile('io.dapr:\(.*\):.*')/compile('io.dapr:\\1:${DAPR_JAVA_SDK_VERSION}')/g" README.md
fi

rm -rf docs
mvn clean install
mvn site-deploy
