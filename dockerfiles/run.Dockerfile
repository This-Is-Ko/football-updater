FROM eclipse-temurin:20.0.2_9-jre-jammy
RUN mkdir /app
COPY /build/libs/app.jar /app/app.jar
WORKDIR /app
EXPOSE 443
ENTRYPOINT ["java","-jar","app.jar","--spring.profiles.active=prod"]