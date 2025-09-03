#!/bin/bash

set -uex

DAPR_JAVA_SDK_VERSION=$1

# Alpha artifacts of the sdk tracks the regular SDK minor and patch versions, just not the major.
# Replaces the SDK major version to 0 for alpha artifacts.
DAPR_JAVA_SDK_ALPHA_VERSION=`echo $DAPR_JAVA_SDK_VERSION | sed 's/^[0-9]*\./0./'`

mvn versions:set -DnewVersion=$DAPR_JAVA_SDK_VERSION

mvn versions:set-property -Dproperty=dapr.sdk.version -DnewVersion=$DAPR_JAVA_SDK_VERSION
mvn versions:set-property -Dproperty=dapr.sdk.alpha.version -DnewVersion=$DAPR_JAVA_SDK_ALPHA_VERSION

###################
# Alpha artifacts #
###################

git clean -f
