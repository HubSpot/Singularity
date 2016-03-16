#!/bin/bash

set -e

gitbook build
cp SingularityUI/app/assets/static/images/favicon.ico _book/gitbook/images/favicon.ico
cd _book
git init
git add .
git commit -m "update gitbook from master branch docs"
git push --force --quiet git@github.com:HubSpot/Singularity.git master:gh-pages