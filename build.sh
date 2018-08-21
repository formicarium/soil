#!/usr/bin/env bash

set -eou pipefail

lein clean
lein uberjar
