# Example: Akka Cluster on Kubernetes

This example app provides an actual example of working Akka Cluster, as such this example app complements the Akka Cluster on Kubernetes guide.

To run the example app, please follow these steps.

## Pre-requisite

* A working Kubernetes installation, i.e. an actual Kubernetes cluster or Minikube.
* Ensure the Docker environment variable is configured to point to the Kubernetes cluster. If using minikube this is achieved by calling `eval $(minikube docker-env)`.

## 1. Build the Docker base image

Build the Docker base image required by the example app:  

```bash
$ cat <<EOF | docker build -t gdiamantidis/openjdk-jre-8-bash:latest -
FROM openjdk:8-jre-alpine
RUN apk --no-cache add --update bash coreutils curl
EOF
```

The example app makes use of `openjdk:8-jre-alpine` as the base image with `bash`, networking utility (i.e. `ping`, `telnet`), and `curl`.


## 2. Build the example app

Ensure you are at the root directory of the example app. Build the example app by running the following command:

```bash
$ sbt docker:publishLocal
```

## 3. Deploy the example app

Deploy the example app by running the following command:

```bash
$ kubectl create -f deploy/kubernetes/resources/myapp
```

## 4. Confirm the example app is working

Check the logs of the pods created by the example app (i.e. `myapp-0`, `myapp-1`, and `myapp-2`.). The `-f` switch follows the logs emitted by the pod.

```bash
$ kubectl logs -f myapp-0
```

Once the app is started within the pod, a log entry similar to the following should be displayed:

```
[INFO] [10/03/2017 03:44:19.758] [myapp-akka.actor.default-dispatcher-17] [akka.cluster.Cluster(akka://myapp)] Cluster Node [akka.tcp://myapp@myapp-0.myapp.default.svc.cluster.local:2551] - Leader is moving node [akka.tcp://myapp@myapp-0.myapp.default.svc.cluster.local:2551] to [Up]
```

Wait for the pods `myapp-0`, `myapp-1`, and `myapp-2` to be at the `Running` state.

Once all these pods are `Running`, query the `/members` endpoint to interrogate the members of the cluster. For example, the following displays the list of members visible from `myapp-0` pod:

```
$ kubectl exec -ti myapp-0 -- curl -v myapp-0:9000/members
*   Trying 172.17.0.2...
* TCP_NODELAY set
* Connected to myapp-0 (172.17.0.2) port 9000 (#0)
> GET /members HTTP/1.1
> Host: myapp-0:9000
> User-Agent: curl/7.57.0
> Accept: */*
>
< HTTP/1.1 200 OK
< Server: akka-http/10.0.10
< Date: Thu, 14 Dec 2017 23:10:28 GMT
< Content-Type: application/json
< Content-Length: 401
<
{
  "members" : [ {
    "address" : "akka.tcp://myapp@myapp-0.myapp.default.svc.cluster.local:2551",
    "status" : "Up",
    "roles" : [ ]
  }, {
    "address" : "akka.tcp://myapp@myapp-1.myapp.default.svc.cluster.local:2551",
    "status" : "Up",
    "roles" : [ ]
  }, {
    "address" : "akka.tcp://myapp@myapp-2.myapp.default.svc.cluster.local:2551",
    "status" : "Up",
    "roles" : [ ]
  } ]
* Connection #0 to host myapp-0 left intact
}
```


https://hub.docker.com/r/gdiamantidis/
https://console.cloud.google.com/kubernetes/list?project=savvy-motif-194111

docker login
(gdiamantidis)

sbt docker:publish

gcloud auth login
gcloud container clusters get-credentials cluster-demo --zone europe-west1-b --project {project-name}

kubectl config get-contexts (confirm context is gcloud project)

kubectl delete -f deploy/kubernetes/resources/myapp
