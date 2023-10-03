#!/bin/bash

set -uex

# The workflows sdk tracks the regular SDK minor and patch versions, just not the major.
# Replaces the workflows SDK major version to 0 until it is stable.
DAPR_JAVA_WORKFLOWS_SDK_VERSION=`echo $DAPR_JAVA_SDK_VERSION | sed 's/^[0-9]*\./0./'`

mvn versions:set -DnewVersion=$DAPR_JAVA_SDK_VERSION
mvn versions:set-property -Dproperty=dapr.sdk.version -DnewVersion=$DAPR_JAVA_SDK_VERSION -f sdk-tests/pom.xml

mvn versions:set -DnewVersion=$DAPR_JAVA_WORKFLOWS_SDK_VERSION -f sdk-workflows/pom.xml
mvn versions:set-property -Dproperty=dapr.sdk-workflows.version -DnewVersion=$DAPR_JAVA_WORKFLOWS_SDK_VERSION

git clean -f
