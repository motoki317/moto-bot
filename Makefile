build:
	mvn clean package

javadoc:
	mvn dependency:resolve -Dclassifier=javadoc
