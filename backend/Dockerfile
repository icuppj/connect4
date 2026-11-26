FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean compile dependency:copy-dependencies -DoutputDirectory=target/dependency

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/classes ./classes
COPY --from=build /app/target/dependency ./dependency
EXPOSE 8080
CMD ["java", "-cp", "classes:dependency/*", "com.connect4.App"]