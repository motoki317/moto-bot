.PHONY: build
build:
	docker-compose -f ./docker-compose.dev.yml up -d --build

.PHONY: up
up:
	docker-compose -f ./docker-compose.dev.yml up -d

.PHONY: down
down:
	docker-compose -f ./docker-compose.dev.yml down

.PHONY: test
test:
	mvn test

.PHONY: db-up
db-up:
	docker-compose -f ./docker-compose.dve.yml up -d mysql

.PHONY: db
db:
	docker-compose -f ./docker-compose.dev.yml exec mysql mysql -uroot -ppassword moto-bot

.PHONY: javadoc
javadoc: # Downloads javadoc
	mvn dependency:resolve -D classifier=javadoc
