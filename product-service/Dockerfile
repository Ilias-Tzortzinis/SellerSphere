FROM openjdk:21

COPY ./target/product-service-1.0.0.jar /app/product-service.jar
COPY ./src/main/resources/prod-application.properties /app/application.properties

ENTRYPOINT ["java"]

CMD ["-jar", "/app/product-service.jar", "--spring.config.location=/app/application.properties"]
