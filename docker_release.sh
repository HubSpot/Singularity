#!/bin/bash
set -e

RELEASE_VERSION=$1
NEW_VERSION=$2

git checkout "Singularity-$RELEASE_VERSION"
mvn clean package docker:build -DskipTests

git checkout master
mvn clean package docker:build -DskipTests

docker tag hubspot/singularityservice:$NEW_VERSION hubspot/singularityservice:latest
docker tag hubspot/singularityexecutorslave:$NEW_VERSION hubspot/singularityexecutorslave:latest
docker push hubspot/singularityservice:$RELEASE_VERSION && docker push hubspot/singularityservice:$NEW_VERSION && docker push hubspot/singularityservice:latest && docker push hubspot/singularityexecutorslave:$RELEASE_VERSION && docker push hubspot/singularityexecutorslave:$NEW_VERSION && docker push hubspot/singularityexecutorslave:latest
