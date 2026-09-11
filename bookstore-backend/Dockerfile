# ---------- Build stage ----------

FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Copy the POM and download dependencies BEFORE copying source code.
# Docker caches each instruction as its own layer; as long as pom.xml
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

# maven.test.skip=true (not just skipTests) skips COMPILING test sources
# too, not just running them — the production image build has no need for
# test classes at all.
RUN mvn clean package -Dmaven.test.skip=true -B

# ---------- Runtime stage ----------
# JRE only, not the full JDK — smaller image, smaller attack surface.
FROM eclipse-temurin:17-jre-alpine


RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown app:app app.jar
USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
