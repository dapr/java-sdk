#!/usr/bin/env bash
#
# Copyright 2024 The Dapr Authors
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#     http://www.apache.org/licenses/LICENSE-2.0
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -ue

script_dir=$(readlink -f $(dirname $0))
current_time=$(date +"%Y-%m-%d_%H-%M-%S")

# Thanks to https://ihateregex.io/expr/semver/
SEMVER_REGEX='^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$'

REL_VERSION=`echo $1 | sed -r 's/^[vV]?([0-9].+)$/\1/'`

if [ `echo $REL_VERSION | pcre2grep "$SEMVER_REGEX"` ]; then
  echo "$REL_VERSION is a valid semantic version."
else
  echo "$REL_VERSION is not a valid semantic version."
  exit 1
fi

MAJOR_MINOR_VERSION=`echo ${REL_VERSION}- | cut -d- -f1 | cut -d. -f1,2`
MAJOR_MINOR_PATCH_VERSION=`echo ${REL_VERSION}- | cut -d- -f1 | cut -d. -f1,2,3`
VARIANT=`echo ${REL_VERSION}- | cut -d- -f2`

if [ "$VARIANT" = "SNAPSHOT" ]; then
  echo "SNAPSHOT release detected, updating version in master branch to $REL_VERSION ..."
  if [ "$REL_VERSION" != "${MAJOR_MINOR_PATCH_VERSION}-SNAPSHOT" ]; then
    echo "Invalid snapshot version: $REL_VERSION"
    exit 3
  fi
  branch_name="automation/update_to_next_${current_time}"
  git checkout -b $branch_name
  ${script_dir}/update_sdk_version.sh $REL_VERSION
  git clean -xdf
  git commit -s -m "Update master version to ${$REL_VERSION}" -a
  git push origin $branch_name
  gh pr create --repo ${GITHUB_REPOSITORY} \
    --base master \
    --title "Update master version to ${$REL_VERSION}" \
    --body "Update master version to ${$REL_VERSION}"
  echo "Done."
  exit 0
elif [ "$VARIANT" = "rc" ]; then
  echo "Release-candidate version detected: $REL_VERSION"
  RC_COUNT=`echo ${REL_VERSION}- | cut -d- -f3`
  if ! ((10#${RC_COUNT} >= 0)) 2>/dev/null || [ "$RC_COUNT" == "" ]; then
    echo "Invalid release-candidate count: $RC_COUNT"
    exit 3
  fi
  if [ "$REL_VERSION" != "${MAJOR_MINOR_PATCH_VERSION}-rc-${RC_COUNT}" ]; then
    echo "Invalid release-candidate version: $REL_VERSION"
    exit 3
  fi
elif [ "$VARIANT" = "" ]; then
  echo "Release version detected: $REL_VERSION"
else
  echo "Invalid release variant: $VARIANT"
  exit 3
fi

echo "Passed all version format validations."

RELEASE_BRANCH="release-$MAJOR_MINOR_VERSION"
RELEASE_TAG="v$REL_VERSION"

if [ `git rev-parse --verify origin/$RELEASE_BRANCH 2>/dev/null` ]; then
  echo "$RELEASE_BRANCH branch already exists, checking it out ..."
  git checkout $RELEASE_BRANCH
else
  echo "$RELEASE_BRANCH does not exist, creating ..."
  git checkout -b $RELEASE_BRANCH
  git push origin $RELEASE_BRANCH
fi
echo "$RELEASE_BRANCH branch is ready."

if [ `git rev-parse --verify $RELEASE_TAG 2>/dev/null` ]; then
  echo "$RELEASE_TAG tag already exists, aborting ..."
  exit 2
fi

${script_dir}/update_sdk_version.sh $REL_VERSION
git commit -s -m "Release $REL_VERSION" -a
if [ "$VARIANT" = "" ]; then
  echo "Generating docs ..."
  ${script_dir}/update_docs.sh $REL_VERSION
  git commit -s -m "Generate updated javadocs for $REL_VERSION" -a
fi
git push origin $RELEASE_BRANCH

echo "Tagging $RELEASE_TAG ..."
git tag $RELEASE_TAG
echo "$RELEASE_TAG is tagged."

echo "Pushing $RELEASE_TAG tag ..."
git push origin $RELEASE_TAG
echo "$RELEASE_TAG tag is pushed."

if [ "$VARIANT" = "" ]; then
  git clean -xdf
  echo "Creating pull request to update docs ..."
  branch_name="automation/update_docs_${current_time}"
  git reset --hard origin/master
  git cherry-pick $RELEASE_TAG
  git push origin $branch_name
  gh pr create --repo ${GITHUB_REPOSITORY} \
    --base master \
    --title "Update master docs for ${$REL_VERSION} release" \
    --body "Update master docs for ${$REL_VERSION} release"
fi

echo "Done."