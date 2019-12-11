build:
	docker-compose up -d --build

run:
	docker-compose up -d

down:
	docker-compose down

test:
	docker-compose exec moto-bot mvn test

javadoc: # Downloads javadoc
	mvn dependency:resolve -Dclassifier=javadoc
