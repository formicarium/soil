#!/usr/bin/env bash

set -eou pipefail

version=$(cat ./resources/base.edn | grep  ':version .*"[0-9\.]*"' | grep -oEi '\d*\.\d*\.\d*')

lein clean
lein uberjar

docker build -t soil:${version} .
docker tag soil:$1 formicarium/soil:${version}
docker tag soil:$1 formicarium/soil:latest
docker push formicarium/soil:${version}
docker push formicarium/soil:latest
