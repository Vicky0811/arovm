FROM eclipse-temurin:21-jre-alpine
LABEL authors="Vicky"

WORKDIR /app

# Install Docker CLI so Java can run 'docker' commands
RUN apk add --no-cache docker-cli

# Match your specific JAR name
COPY --from=build arovm-v2.0.jar .

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "arovm-v2.0.jar"]
