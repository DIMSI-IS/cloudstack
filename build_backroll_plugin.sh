mvn install -f "/opt/cloudstack/plugins/backup/backroll/pom.xml" -DskipTests
mvn install -f "/opt/cloudstack/plugins/pom.xml" -DskipTests
mvn install -f "/opt/cloudstack/client/pom.xml" -DskipTests
mvn -pl :cloud-client-ui jetty:run 