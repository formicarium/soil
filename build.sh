#!/usr/bin/env bash

set -eou pipefail

lein clean
lein uberjar

docker build -t soil:$1 .
docker tag soil:$1 formicarium/soil:$1
docker tag soil:$1 formicarium/soil:latest
docker push formicarium/soil:$1
docker push formicarium/soil:latest
