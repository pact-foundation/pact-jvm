#!/bin/bash

set -e

if [ -z "$1" ]; then
  echo A description of the release is required
  exit 1
fi

#git merge master

#./gradlew clean check install

VERSION=$(cat build.gradle| awk "/version = '[0-9]+\.[0-9]+\.[0-9]+'/{ match(\$0, /[0-9]+\.[0-9]+\.[0-9]+/); print substr(\$0, RSTART, RLENGTH) }")
PREV_TAG=$(git describe --abbrev=0 --tags)
CHANGELOG=$(git log --pretty='* %h - %s (%an, %ad)' $PREV_TAG..HEAD | tr '\n' '#n')

echo $CHANGELOG
sed -e 1aX -f CHANGELOG.md | head
