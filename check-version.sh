#!/usr/bin/env bash

set -eou pipefail

version=$(cat ./resources/base.edn | grep  ':version .*"[-0-9\.a-zA-Z]*"' | grep -oEi '[0-9]+\.[0-9]+\.[0-9]+-beta[0-9]*')
status_code=$(curl -s -o /dev/null -w "%{http_code}" https://hub.docker.com/v2/repositories/formicarium/soil/tags/${version}/)

if [ ${status_code} -eq 200 ]; then
 echo "Version ${version} already present in the remote registry!"
 exit 1
fi;

echo "Version accepted"
