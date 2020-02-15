FROM maven:3.6.3-jdk-8

WORKDIR /usr/src/moto-bot

COPY ./pom.xml ./
RUN mvn dependency:resolve

COPY . .
RUN mvn clean package -D skipTests

EXPOSE 8080

ENTRYPOINT ["./target/bin/main"]
