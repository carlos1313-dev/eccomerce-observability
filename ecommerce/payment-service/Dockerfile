FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.3.0/opentelemetry-javaagent.jar otel-agent.jar

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8082

ENV JAVA_OPTS="-javaagent:/app/otel-agent.jar \
               -Dotel.service.name=payment-service \
               -Dotel.exporter.otlp.endpoint=http://lgtm:4318 \
               -Dotel.exporter.otlp.protocol=http/protobuf \
               -Dotel.logs.exporter=otlp \
               -Dotel.metrics.exporter=otlp \
               -Dotel.traces.exporter=otlp"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
