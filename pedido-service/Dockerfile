FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

COPY . .

RUN mvn -pl pedido-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/pedido-service/target/*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
