# Estágio de Build
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copia todo o projeto para o contexto de build
COPY . .

# Compila apenas o módulo 'produto-service' e suas dependências,
# executando os testes e as verificações de qualidade.
RUN mvn -pl produto-service -am package

# Estágio de Execução
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copia o JAR gerado do módulo específico
COPY --from=build /app/produto-service/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
