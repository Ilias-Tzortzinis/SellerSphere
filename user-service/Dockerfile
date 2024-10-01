FROM openjdk:23

COPY ./target/user-service-0.0.1-SNAPSHOT.jar /app/user-service.jar
COPY ./application-prod.properties /app/application.properties

ENTRYPOINT ["java"]
CMD ["-jar", "/app/user-service.jar", "--spring.config.location=/app/application.properties"]