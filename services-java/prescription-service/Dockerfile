# Build context: repository root
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY . .
RUN mvn -q -pl services-java/prescription-service -am -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S hospital && adduser -S hospital -G hospital
COPY --from=build /build/services-java/prescription-service/target/prescription-service-1.0.0.jar app.jar
USER hospital
EXPOSE 8088
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
