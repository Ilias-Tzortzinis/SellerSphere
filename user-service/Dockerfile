FROM openjdk:21

COPY ./user-service/target/user-service-1.0.0.jar /app/user-service.jar
COPY ./docker/user-service/application.properties /app/application.properties

WORKDIR /app

ENTRYPOINT ["java"]

CMD ["-jar", "user-service.jar", "--spring.config.location=/app/application.properties"]