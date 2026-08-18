FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /home/app

COPY . .

RUN mvn clean package -DskipTests


FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /home/app/plugins/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]