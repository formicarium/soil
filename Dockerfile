FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/soil-0.0.1-SNAPSHOT-standalone.jar /soil/app.jar

EXPOSE 8080

CMD ["java", "-jar", "/soil/app.jar"]
