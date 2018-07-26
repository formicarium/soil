#!/usr/bin/env bash
set -eou pipefail

lein uberjar

echo "Cleaning previous version..."
docker rmi soil:local || echo "No previous version to clear."

echo "Building soil:local"
docker build --tag soil:local .
