FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar coc-clan-bot.jar
ENTRYPOINT ["sh", "-c", "rm -f /tmp/coc-clan-bot.ready && exec java -jar /app/coc-clan-bot.jar"]
