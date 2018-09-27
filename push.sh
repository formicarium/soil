#!/usr/bin/env bash

set -eou pipefail

version=$(cat ./resources/base.edn | grep  ':version .*"[0-9\.]*"' | grep -oEi '[0-9]+\.[0-9]+\.[0-9]+')
docker build -t soil:${version} .
docker tag soil:${version} formicarium/soil:${version}
docker tag soil:${version} formicarium/soil:latest
docker push formicarium/soil:${version}
docker push formicarium/soil:latest
