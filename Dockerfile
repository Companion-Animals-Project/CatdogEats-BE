FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE=build/libs/*.jar

WORKDIR /app

COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","/app/app.jar"]
