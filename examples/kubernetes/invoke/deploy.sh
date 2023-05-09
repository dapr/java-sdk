#!/bin/bash

# Do not exit on errors
set +e

apps=${1:-all}

if [[ -z $IMAGE_REGISTRY ]]; then
  echo "env IMAGE_REGISTRY is required, exiting"
  exit -1
fi

# Delete existing deployments first, then we apply the deployments for each:
if [[ "$apps" == "svc" || "$apps" == "all" ]]; then
  set -x
  sed -e "s|\$IMAGE_REGISTRY|$IMAGE_REGISTRY|g" hello-grpc-svc.yml | kubectl delete -f -
  sed -e "s|\$IMAGE_REGISTRY|$IMAGE_REGISTRY|g" hello-grpc-svc.yml | kubectl apply -f -
  set +x
fi

if [[ "$apps" == "spring" || "$apps" == "all" ]]; then
  set -x
  sed -e "s|\$IMAGE_REGISTRY|$IMAGE_REGISTRY|g" hello-grpc-spring-app.yml | kubectl delete -f -
  sed -e "s|\$IMAGE_REGISTRY|$IMAGE_REGISTRY|g" hello-grpc-spring-app.yml | kubectl apply -f -
  set +x
fi
