FROM maven:3.6.3-jdk-8

WORKDIR /usr/src/moto-bot

COPY ./pom.xml ./
COPY pom.xml /tmp/pom.xml
RUN mvn -B -f /tmp/pom.xml -s /usr/share/maven/ref/settings-docker.xml dependency:resolve

COPY . .
RUN mvn -B -f ./pom.xml -s /usr/share/maven/ref/settings-docker.xml clean package -D skipTests

EXPOSE 8080

ENTRYPOINT ./target/bin/main
