# syntax=docker/dockerfile:1

# ---------- Estagio 1: build ----------
# Imagem com Maven + JDK 21 usada apenas para compilar; nao vai para a imagem final.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Copia so o pom primeiro: as dependencias ficam em cache e so sao baixadas de novo se o pom mudar
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Os testes rodam na etapa "test" do pipeline; aqui so empacotamos
RUN mvn -B -q package -DskipTests \
    && cp target/inovagab-backend-*.jar app.jar

# ---------- Estagio 2: runtime ----------
# Apenas o JRE (sem compilador nem Maven): imagem menor e com menos superficie de ataque
FROM eclipse-temurin:21-jre-alpine

LABEL org.opencontainers.image.title="inovagab-backend" \
      org.opencontainers.image.description="API REST da plataforma de inovacao InovaGAB" \
      org.opencontainers.image.vendor="FIAP - Challenge InovaGAB"

# Usuario sem privilegios de root
RUN addgroup -S inovagab && adduser -S inovagab -G inovagab
WORKDIR /app
COPY --from=build --chown=inovagab:inovagab /build/app.jar app.jar
USER inovagab

ENV SERVER_ADDRESS=0.0.0.0 \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseContainerSupport"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
