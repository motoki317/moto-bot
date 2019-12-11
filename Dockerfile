FROM maven:3.6.3-jdk-8

WORKDIR /usr/src/moto-bot
COPY . .

RUN mvn clean package

EXPOSE 8080

ENTRYPOINT ./target/bin/main
