#!/bin/bash
set -e

git checkout hs_staging
git pull
git merge master --no-edit
git push hs_staging

git checkout hs_qa
git pull
git merge master --no-edit
git push hs_qa

git checkout hs_stable
git pull
git merge master --no-edit
git push hs_stable