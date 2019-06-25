#!/bin/bash
# ----------------------------------------------------------
# Automatically push back the generated JavaDocs to gh-pages
# ----------------------------------------------------------
# based on https://gist.github.com/willprice/e07efd73fb7f13f917ea

# specify the common address for the repository
targetRepo=github.com/akarnokd/RxJavaExtensions.git
# =======================================================================

# only for main pushes, for now
# if [ "$TRAVIS_PULL_REQUEST" == "true" ] || [ "$TRAVIS_TAG" == "" ]; then
if [ "$TRAVIS_PULL_REQUEST" == "true" ]; then
	echo -e "Pull request detected, skipping JavaDocs pushback."
	exit 0
fi

# check if the token is actually there
if [ "$GITHUB_TOKEN" == "" ]; then
	echo -e "No access to GitHub, skipping JavaDocs pushback."
	exit 0
fi

# prepare the git information
git config --global user.email "travis@travis-ci.org"
git config --global user.name "Travis CI"

# setup the remote
git remote add origin-pages https://${GITHUB_TOKEN}@${targetRepo} > /dev/null 2>&1

# stash changes due to chmod
git stash

# get the gh-pages
git fetch --all
git branch -a
git checkout -b gh-pages origin-pages/gh-pages

# remove old dir
rm -rf javadoc

# copy and overwrite new doc
yes | cp -rfv ./build/docs/javadoc/ javadoc/

# stage all changed and new files
git add *.html
git add *.css
git add *.js
git add javadoc/package-list

# commit all
git commit --message "Travis build: $TRAVIS_BUILD_NUMBER"


# push it
git push --quiet --set-upstream origin-pages gh-pages

# we are done
