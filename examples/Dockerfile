FROM eclipse-temurin:11

ENV MAIN_CLAZZ=io.dapr.examples.invoke.grpc.HelloWorldService
ENV PORT=5000

RUN mkdir /app
COPY target/dapr-java-sdk-examples-exec.jar /app/app.jar
ENTRYPOINT [ "java", "-jar", "/app/app.jar" ]
CMD [ "io.dapr.examples.invoke.grpc.HelloWorldService", "-p", "5000" ]
