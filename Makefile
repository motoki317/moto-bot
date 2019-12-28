build:
	docker-compose up -d --build

up:
	docker-compose up -d

down:
	docker-compose down

test:
	docker-compose exec moto-bot mvn test

db-up:
	docker-compose up -d mysql

db:
	docker-compose exec mysql mysql -u root -p

javadoc: # Downloads javadoc
	mvn dependency:resolve -Dclassifier=javadoc
