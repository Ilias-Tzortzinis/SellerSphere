FROM openjdk:21

COPY ./target/cart-service-1.0.0.jar /app/cart-service.jar
COPY ./src/main/resources/prod-application.properties /app/application.properties

ENTRYPOINT ["java"]

CMD ["-jar", "/app/cart-service.jar", "--spring.config.location=/app/application.properties"]