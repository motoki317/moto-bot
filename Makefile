.PHONY: build
build:
	docker-compose -f ./docker-compose.dev.yml up -d --build

.PHONY: up
up:
	docker-compose -f ./docker-compose.dev.yml up -d

.PHONY: down
down:
	docker-compose -f ./docker-compose.dev.yml down

.PHONY: test-db-up
up-test-db:
	docker run \
			--rm --name moto-bot-test-db \
            -e MYSQL_ROOT_PASSWORD=password \
            -e MYSQL_USER=moto-bot -e MYSQL_PASSWORD=password \
            -e MYSQL_DATABASE=moto-bot_test \
            -v $(PWD)/mysql/init:/docker-entrypoint-initdb.d \
            -d -p "3306:3306" mariadb:10.5.11

.PHONY: test-db-down
down-test-db:
	docker stop moto-bot-test-db

.PHONY: test
test:
	mvn test

.PHONY: db-up
db-up:
	docker-compose -f ./docker-compose.dev.yml up -d mysql

.PHONY: db
db:
	docker-compose -f ./docker-compose.dev.yml exec mysql mysql -uroot -ppassword moto-bot

.PHONY: javadoc
javadoc: # Downloads source (and javadoc)
	mvn dependency:sources
	mvn dependency:resolve -D classifier=javadoc
