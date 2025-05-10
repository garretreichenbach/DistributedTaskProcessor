FROM openjdk:21-jdk-slim
WORKDIR /app
COPY build/libs/DistributedTaskProcessor.jar /app/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "DistributedTaskProcessor.jar"]