build:
	docker-compose up -d --build

run:
	docker-compose up -d

javadoc: # Downloads javadoc
	mvn dependency:resolve -Dclassifier=javadoc
