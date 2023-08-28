FROM amazoncorretto:17.0.8-al2023-headless as builder
WORKDIR /app
COPY . .
RUN chmod +x mvnw
RUN ./mvnw clean package -DskipTests

FROM amazoncorretto:17-alpine3.18
WORKDIR /app
COPY --from=builder /app/target/simplemail-0.0.1-SNAPSHOT.jar ./app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
