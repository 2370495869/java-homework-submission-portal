FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
RUN chmod +x mvnw
RUN ./mvnw -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /var/lib/homework/uploads \
    && chown -R 10001:10001 /var/lib/homework /app
COPY --from=build --chown=10001:10001 /workspace/target/homework-submission-portal.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
