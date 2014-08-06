#!/bin/bash

cd SingularityUI/
brunch build

cd ..
mvn clean package
