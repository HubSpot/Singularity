#!/bin/bash

set -e

mvn -Pbuild-swagger-documentation -DskipTests=true -B -q -fae install
python Docs/split_api_docs.py
gitbook install
gitbook build
cp SingularityUI/app/assets/static/images/favicon.ico _book/gitbook/images/favicon.ico
cd _book
echo 'getsingularity.com' > CNAME
rm -rf Docs/releases/*.md
rm -rf Docs/reference/*.md
rm -rf README.md
git init
git add .
git commit -m "update gitbook from master branch docs"
git push --force --quiet git@github.com:HubSpot/Singularity.git master:gh-pages
