FROM maven:3.9.4-openjdk-21 AS build

WORKDIR /work

COPY ./pom.xml ./
# Resole bot dependencies
RUN --mount=type=cache,target=/root/.m2 mvn dependency:resolve

COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn package -D skipTests

FROM alpine:latest AS build-mod

WORKDIR /work

RUN apk add --no-cache wget zip

COPY --from=build /work/target/moto-bot-jar-with-dependencies.jar ./moto-bot.jar

# add libconnector.so for aarch64
RUN mkdir -p natives/linux-aarch64 && \
    wget -O natives/linux-aarch64/libconnector.so "https://github.com/aikaterna/lavaplayer-natives/blob/original/src/main/resources/natives/linux-aarch64/libconnector.so?raw=true" && \
    zip moto-bot.jar natives/linux-aarch64/libconnector.so && \
    rm -r natives

FROM openjdk:21-slim AS runtime

WORKDIR /work

COPY --from=build-mod /work/moto-bot.jar ./moto-bot.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "./moto-bot.jar"]
