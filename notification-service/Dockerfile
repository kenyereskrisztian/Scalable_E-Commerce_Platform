FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -B -pl notification-service -am -DskipTests package

FROM eclipse-temurin:17-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/notification-service/target/*.jar app.jar
EXPOSE 8086
ENV JAVA_TOOL_OPTIONS="-Xms128m -Xmx256m"
ENTRYPOINT ["java", "-jar", "app.jar"]
