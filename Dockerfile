FROM openjdk:21-jdk-slim
WORKDIR /app
COPY target/task-processor.jar /app/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "task-processor.jar"]