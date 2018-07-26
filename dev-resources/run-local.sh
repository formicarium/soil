#!/usr/bin/env bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null && pwd )"

docker run -d \
            -v $DIR/kube:/root/.kube              \
            -v $HOME/.minikube/:/root/.minikube     \
            --name soil                             \
            soil:local
