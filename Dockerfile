FROM maven:3.5.4-jdk-8 as builder
COPY pom.xml /java/
COPY src /java/src
WORKDIR /java
RUN mvn compile package

FROM openjdk:8-alpine
COPY --from=builder /java/target/k8s-java-load-test-1.0-SNAPSHOT.jar /load-test.jar
CMD ["java", "-jar", "/load-test.jar"]
