# Bookstore Backend

REST API for the Online Bookstore, built with Spring Boot 4.1.1 and Java 17.


## Requirements

- Java 17 or later
- **Lombok plugin/agent installed in your IDE**, if you're using one (IntelliJ:
  built in or one-click plugin install. Eclipse: run `java -jar lombok.jar`
  from your local `.m2` repository and follow the installer — see the main
  project README for the full walkthrough). Building from the command line
  via `./mvnw` does **not** require this.

Maven itself is **not** required — this project includes the Maven Wrapper.

## Running the application

```bash
cd bookstore-backend
./mvnw spring-boot:run        # macOS/Linux
mvnw.cmd spring-boot:run       # Windows
```

Starts at `http://localhost:8080`. Verify it's up:

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

## Running tests

```bash
./mvnw test
```
