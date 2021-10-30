FROM maven:3.8.1-openjdk-17 AS build

WORKDIR /usr/src/moto-bot

COPY ./pom.xml ./
# Resole bot dependencies
RUN --mount=type=cache,target=/root/.m2 mvn dependency:resolve

COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn package -D skipTests

FROM openjdk:17-slim AS runtime

WORKDIR /usr/src/moto-bot

COPY --from=build /usr/src/moto-bot/target/moto-bot-jar-with-dependencies.jar ./moto-bot.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "--enable-preview", "./moto-bot.jar"]
