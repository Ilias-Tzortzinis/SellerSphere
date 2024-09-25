FROM openjdk:21

COPY ./target/user-service-1.0.0.jar /app/user-service.jar
COPY ./src/main/resources/prod-application.properties /app/application.properties

ENTRYPOINT ["java"]

CMD ["-jar", "/app/user-service.jar", "--spring.config.location=/app/application.properties"]