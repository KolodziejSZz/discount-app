FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./pom.xml
COPY discount-app-domain/pom.xml ./discount-app-domain/pom.xml
COPY discount-app-application/pom.xml ./discount-app-application/pom.xml
COPY discount-app-infrastructure/pom.xml ./discount-app-infrastructure/pom.xml
COPY discount-app-bootstrap/pom.xml ./discount-app-bootstrap/pom.xml
RUN mvn dependency:go-offline -B || true
COPY . .
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/discount-app-bootstrap/target/*.jar app.jar
RUN chown -R app:app /app
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
