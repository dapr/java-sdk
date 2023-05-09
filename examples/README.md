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
```sh
cd kubernetes/invoke
sed -e "s|\$IMAGE_REGISTRY|$IMAGE_REGISTRY|g" hello-grpc-svc.yml | kubectl apply -f -
sed -e "s|\$IMAGE_REGISTRY|$IMAGE_REGISTRY|g" hello-grpc-spring-app.yml | kubectl apply -f -
```

Check the rollout status:
```sh
kubectl -n dapr-playground rollout status deploy/hello-grpc-java-svc
kubectl -n dapr-playground rollout status deploy/hello-grpc-spring-app
```

Check the logs:
```sh
kubectl -n dapr-playground logs \
  --selector=app=hello-grpc-spring-app \
  -c dapr-java-examples \
  --tail=-1
```

Expose the service port to outside world:

> There are several different ways to access a Kubernetes service depending on which platform you are using. Port forwarding is one consistent way to access a service, whether it is hosted locally or on a cloud Kubernetes provider like AKS.

```sh
kubectl -n dapr-playground port-forward service/hello-grpc-spring-app 30007:3000
# This will print below logs if successful:
Forwarding from 127.0.0.1:30007 -> 3000
Forwarding from [::1]:30007 -> 3000
```

Now we can access the exposed service at `127.0.0.1:30007` from browser.
For example, access `http://127.0.0.1:30007/invoke/grpc/say` to invoke the REST API to trigger GRPC call.
