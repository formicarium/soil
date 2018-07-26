FROM java:8-alpine

#https://github.com/lachie83/k8s-kubectl/blob/v1.11.1/Dockerfile
ENV KUBE_LATEST_VERSION="v1.11.1"

RUN apk add --update ca-certificates \
 && apk add --update -t deps curl \
 && curl -L https://storage.googleapis.com/kubernetes-release/release/${KUBE_LATEST_VERSION}/bin/linux/amd64/kubectl -o /usr/local/bin/kubectl \
 && chmod +x /usr/local/bin/kubectl \
 && apk del --purge deps \
 && rm /var/cache/apk/*

ADD target/soil-0.0.1-SNAPSHOT-standalone.jar /soil/app.jar

EXPOSE 8080

VOLUME ["$HOME/.kube"]

CMD ["java", "-jar", "/soil/app.jar"]
