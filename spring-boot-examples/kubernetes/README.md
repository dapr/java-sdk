# Running this example on Kubernetes

To run this example on Kubernetes, you can use any Kubernetes distribution. 
We install Dapr on a Kubernetes cluster and then we will deploy both the `producer-app` and `consumer-app`.

## Creating a cluster and installing Dapr

If you don't have any Kubernetes cluster you can use Kubernetes KIND to create a local cluster. We will create a cluster 
with a local container registry, so we can push our container images to it. This is covered in the 
[KIND documentation here](https://kind.sigs.k8s.io/docs/user/local-registry/).

```bash
./kind-with-registry.sh
```

**Note**: If you are using Podman Desktop, instead of Docker you need to run the following command to enable insecure registries:

```
read -r -d '' registry_conf <<EOF
[[registry]]
location = "localhost:5001"
insecure = true
EOF
podman machine ssh --username=root sh -c 'cat > /etc/containers/registries.conf.d/local.conf' <<<$registry_conf
```

Once you have the cluster up and running you can install Dapr: 

```bash
helm repo add dapr https://dapr.github.io/helm-charts/
helm repo update
helm upgrade --install dapr dapr/dapr \
--version=1.17.0-rc.8 \
--namespace dapr-system \
--create-namespace \
--wait
```

## Creating containers using Spring Boot and pushing to local registry

Now that we have our cluster set up with a local container registry, we need to build our `producer-app` and `consumer-app` containers.
For this we will use Spring Boot build it functions to create container images using [Buildpacks](https://buildpacks.io): 

From inside the `spring-boot-examples/producer-app` directory you can run the following command to create a container: 
```bash
mvn spring-boot:build-image
```

Once we have the container image created, we need to tag and push to the local registry, so the image can be used from our local cluster.
Alternatively, you can push the images to a public registry and update the Kubernetes manifests accordingly. 

```bash
docker tag producer-app:0.15.0-SNAPSHOT localhost:5001/sb-producer-app
docker push localhost:5001/sb-producer-app
```

**Note**: for Podman you need to run: 
```
podman push localhost:5001/sb-producer-app --tls-verify=false
```

From inside the `spring-boot-examples/consumer-app` directory you can run the following command to create a container:
```bash
mvn spring-boot:build-image
```

Once we have the container image created, we need to tag and push to the local registry, so the image can be used from our local cluster.
Alternatively, you can push the images to a public registry and update the Kubernetes manifests accordingly.

```bash
docker tag consumer-app:0.15.0-SNAPSHOT localhost:5001/sb-consumer-app
docker push localhost:5001/sb-consumer-app
```

**Note**: for Podman you need to run: 
```
podman push localhost:5001/sb-consumer-app --tls-verify=false
```

Now we are ready to install our application into the cluster.

## Installing and interacting with the application

Now that we have a running Kubernetes cluster, we need to first install the components needed by the application. 
In this case RabbitMQ and PostgreSQL. We will use Helm to do so: 

Let's start with RabbitMQ:
```bash
helm install rabbitmq  oci://registry-1.docker.io/bitnamicharts/rabbitmq --set auth.username=guest --set auth.password=guest --set auth.erlangCookie=ABC
```

Then PostgreSQL: 
```bash
helm install postgresql oci://registry-1.docker.io/bitnamicharts/postgresql --set global.postgresql.auth.database=dapr --set global.postgresql.auth.postgresPassword=password
```

Once we have these components up and running we can install the application by running from inside 
the `spring-boot-examples/kubernetes/` directory: 

```bash
kubectl apply -f .
```

Next you need to use `kubectl port-forward` to be able to send requests to the applications. 

```bash
kubectl port-forward svc/producer-app 8080:8080
```

In a different terminals you can check the logs of the `producer-app` and `consumer-app`:

```bash
kubectl logs -f producer-app-<POD_ID>
```
and

```bash
kubectl logs -f consumer-app-<POD_ID>
```


