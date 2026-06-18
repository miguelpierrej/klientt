# Build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
RUN ./mvnw -B -DskipTests clean package

# Run
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/klientt-0.0.1-SNAPSHOT.jar app.jar
# Railway injeta $PORT; a app lê-o via server.port=${PORT:8080}.
EXPOSE 8080
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
