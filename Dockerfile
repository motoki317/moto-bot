FROM maven:3.6.3-jdk-8 AS build

WORKDIR /usr/src/moto-bot

COPY ./pom.xml ./
# Resole bot dependencies
RUN mvn dependency:resolve
# Resolve build dependencies
RUN mvn package

COPY ./src ./src
RUN mvn package -D skipTests

FROM openjdk:8-jre-slim AS runtime

WORKDIR /usr/src/moto-bot

COPY --from=build /usr/src/moto-bot/target/moto-bot-jar-with-dependencies.jar ./moto-bot.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "./moto-bot.jar"]
