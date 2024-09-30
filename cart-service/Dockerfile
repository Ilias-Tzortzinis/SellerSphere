FROM openjdk:23

COPY ./target/cart-service-0.0.1-SNAPSHOT.jar /app/cart-service.jar
COPY ./application-prod.properties /app/application.properties

ENTRYPOINT ["java"]
CMD ["-jar", "/app/cart-service.jar", "--spring.config.location=/app/application.properties"]