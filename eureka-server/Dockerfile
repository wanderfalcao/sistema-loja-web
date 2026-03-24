# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia todo o projeto para o contexto de build
COPY . .

# Compila apenas o módulo 'eureka-server' e suas dependências
RUN mvn -pl eureka-server -am package

# Estágio de Execução
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o JAR gerado do módulo específico
COPY --from=build /app/eureka-server/target/*.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
