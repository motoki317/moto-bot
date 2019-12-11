build:
	docker-compose up -d --build

run:
	docker-compose up -d

test:
	docker-compose exec moto-bot mvn test

javadoc: # Downloads javadoc
	mvn dependency:resolve -Dclassifier=javadoc
