# Build context: repository root
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN mvn -q -pl services-java/patient-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S hospital && adduser -S hospital -G hospital
COPY --from=build /build/services-java/patient-service/target/patient-service-1.0.0.jar app.jar
USER hospital
EXPOSE 8082
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
