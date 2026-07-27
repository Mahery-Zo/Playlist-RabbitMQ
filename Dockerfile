# Multi-stage Docker build for Naina Playlist API
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build

# Copier les fichiers de projet et POMs
COPY pom.xml .
COPY common/pom.xml common/
COPY watcher/pom.xml watcher/
COPY metadata/pom.xml metadata/
COPY uploader/pom.xml uploader/
COPY api/pom.xml api/

# Copier les sources
COPY common/src common/src
COPY api/src api/src

# Construire uniquement le module API avec limite mémoire optimisée pour Render
RUN MAVEN_OPTS="-Xmx512m" mvn clean package -pl api -am -DskipTests

# Stage final : runtime avec image LTS
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Créer les dossiers de stockage et de logs
RUN mkdir -p storage/songs logs

# Copier le jar construit
COPY --from=builder /build/api/target/api.jar app.jar

# Configuration des variables d'environnement par défaut
ENV API_PORT=8090
ENV STORAGE_DIR=/app/storage/songs
ENV JAVA_OPTS="-Xmx256m"

EXPOSE 8090

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
