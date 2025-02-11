# Running this example on Kubernetes

To run this example on Kubernetes, you can use any Kubernetes distribution. 
We install Dapr on a Kubernetes cluster and then we will deploy both the `producer-app` and `consumer-app`.

## Creating a cluster and installing Dapr

If you don't have any Kubernetes cluster you can use Kubernetes KIND to create a local cluster. 

```bash
kind create cluster
```

Once you have the cluster up and running you can install Dapr: 

```bash
helm repo add dapr https://dapr.github.io/helm-charts/
helm repo update
helm upgrade --install dapr dapr/dapr \
--version=1.14.4 \
--namespace dapr-system \
--create-namespace \
--wait
```

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

Once we have these components up and running we can install the application by running: 

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


