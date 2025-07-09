ARG JAR_FILE=build/libs/*.jar

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-Dfile.encoding=UTF-8","-jar","/app/app.jar"]
