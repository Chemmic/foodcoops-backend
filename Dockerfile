FROM maven:3.9.16-eclipse-temurin-11 AS build
WORKDIR /home/app

# Kopiere das gesamte Projektverzeichnis
COPY . .

# Baue das Projekt mit Maven Wrapper
RUN mvn clean install -DskipTests

# Verwende das offizielle OpenJDK Image, um die Jar auszuführen
FROM eclipse-temurin:11-jre
COPY --from=build /home/app/plugins/target/*.jar /usr/local/lib/app.jar
ENTRYPOINT ["java","-jar","/usr/local/lib/app.jar"]
