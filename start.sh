#!/bin/bash

CWD=$(pwd)
echo "Starting singularity in $CWD.."
java -jar SingularityService/target/SingularityService-*-SNAPSHOT.jar server vagrant_singularity.yaml
