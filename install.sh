onos-app localhost uninstall org.ece595.widest
mvn clean install -DskipTests
onos-app localhost install! target/widest-path-app-1.0-SNAPSHOT.oar