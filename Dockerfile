FROM eclipse-temurin:25-jdk
COPY target/secure-0.0.1-SNAPSHOT.jar secure.jar
ENTRYPOINT [ "java", "-jar", "/secure.jar"]