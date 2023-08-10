#!/bin/bash

set -uex

DAPR_JAVA_SDK_VERSION=$1
# replace the major version of DAPR_JAVA_SDK_VERSION with 0 while in alpha
DAPR_WORKFLOW_SDK_VERSION=$(echo $DAPR_JAVA_SDK_VERSION | sed -E "s/[0-9]+\.(.*?)/0.\1/")

mvn versions:set -DnewVersion=$DAPR_JAVA_SDK_VERSION
mvn versions:set-property -Dproperty=dapr.sdk.version -DnewVersion=$DAPR_JAVA_SDK_VERSION -f sdk-tests/pom.xml

mvn versions:set -DnewVersion=$DAPR_WORKFLOW_SDK_VERSION -f sdk-workflows/pom.xml
mvn versions:set-property -Dproperty=dapr.sdk-workflows.version -DnewVersion=$DAPR_WORKFLOW_SDK_VERSION
