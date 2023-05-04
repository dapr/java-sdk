# Dapr Java SDK Examples

## Pre-Requisites

* Follow [java-sdk/README.md](../README.md) to build the jars.
* Create the `dapr-playground` namespace: `kubectl create ns dapr-playground`

## Getting Started with Kubernetes

### Build docker image
```sh
docker build -t dapr-java-examples -f Dockerfile .
```

### Push to docker hub image registry
> ... or any other registries

```sh
docker image tag dapr-java-examples ${DOCKER_USERNAME}/dapr-java-examples
docker image push ${DOCKER_USERNAME}/dapr-java-examples
```

### Deploy to Kubernetes

Set your `IMAGE_REGISTRY` you just pushed to, for example:
```sh
export IMAGE_REGISTRY="docker.io/$DOCKER_USERNAME"
```

Apply the deployments:
```
# kubectl apply -f examples/kubernetes/invoke
cd kubernetes/invoke
sed -e 's|\$IMAGE_REGISTRY|docker.io/huhuaishun|g' hello-grpc-svc.yml    | kubectl apply -f -
sed -e 's|\$IMAGE_REGISTRY|docker.io/huhuaishun|g' hello-grpc-client.yml | kubectl apply -f -
```

Check the rollout status:
```
kubectl -n dapr-playground rollout status deploy/hello-grpc-java-svc
kubectl -n dapr-playground rollout status deploy/hello-grpc-java-client
```

Check the logs:
```
kubectl -n dapr-playground logs \
  --selector=app=hello-grpc-java-client \
  -c dapr-java-examples \
  --tail=-1
```
