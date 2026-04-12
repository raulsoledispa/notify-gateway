# ==========================================
# STAGE 1: Build Stage
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

# Set the working directory inside the build container
WORKDIR /app

# Copy only the POM first to cache the dependency downloads
# This makes subsequent builds much faster if the POM hasn't changed.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy the source code
COPY src ./src

# Compile the application (skipping tests during Docker build is typical,
# assuming tests ran in a separate CI/CD pipeline step)
RUN mvn clean package -DskipTests

# Copy runtime dependencies into a distinct folder so we don't need Maven in the final image
RUN mvn dependency:copy-dependencies -DoutputDirectory=target/lib


# ==========================================
# STAGE 2: Secure Runtime Stage
# ==========================================
FROM eclipse-temurin:21-jre-alpine AS runtime

# 1. SECURITY: Create a non-root user and group
# Running as root inside a container is a major security vulnerability.
RUN addgroup -S nova && adduser -S novauser -G nova

# 2. SECURITY: Switch to the non-root user for all subsequent commands
USER novauser:nova

# Set the working directory for the runtime container
WORKDIR /app

# 3. Copy only the necessary compiled artifacts from the build stage
# Chown them simultaneously so our non-root user has ownership.
COPY --from=build --chown=novauser:nova /app/target/lib /app/lib
COPY --from=build --chown=novauser:nova /app/target/notify-gateway-1.0-SNAPSHOT.jar /app/notify-gateway.jar

# Run the application defining the classpath explicitly
# (Since the default pom.xml does not use an Uber-Jar/Shade plugin)
ENTRYPOINT ["java", "-cp", "notify-gateway.jar:lib/*", "com.nova.Main"]
