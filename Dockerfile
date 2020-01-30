FROM openjdk:11.0-jdk
WORKDIR /app

COPY gradle /app/gradle
COPY src /app/src
COPY build.gradle settings.gradle gradlew /app/

RUN ./gradlew shadowJar

FROM openjdk:11.0-jre
WORKDIR /app
RUN mkdir out
COPY --from=0 /app/build/libs/Dcn.jar . 
COPY config.txt .
ENTRYPOINT ["java", "-jar", "Dcn.jar"]

