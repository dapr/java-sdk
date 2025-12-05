#!/bin/bash

set -uex

DAPR_JAVA_SDK_VERSION=$1

# Alpha artifacts of the sdk tracks the regular SDK minor and patch versions, just not the major.
# Replaces the SDK major version to 0 for alpha artifacts.
DAPR_JAVA_SDK_ALPHA_VERSION=`echo $DAPR_JAVA_SDK_VERSION | sed 's/^[0-9]*\./0./'`

if [[ "$OSTYPE" == "darwin"* ]]; then
  sed -i bak "s/<version>.*<\/version>\$/<version>${DAPR_JAVA_SDK_VERSION}<\/version>/g" README.md
  sed -i bak "s/compile('io.dapr:\(.*\):.*')/compile('io.dapr:\\1:${DAPR_JAVA_SDK_VERSION}')/g" README.md
  rm README.mdbak
else
  sed -i "s/<version>.*<\/version>\$/<version>${DAPR_JAVA_SDK_VERSION}<\/version>/g" README.md
  sed -i "s/compile('io.dapr:\(.*\):.*')/compile('io.dapr:\\1:${DAPR_JAVA_SDK_VERSION}')/g" README.md
fi

rm -rf docs
./mvnw -Dmaven.test.skip=true -Djacoco.skip=true clean install
./mvnw -Dmaven.test.skip=true -Djacoco.skip=true site-deploy
