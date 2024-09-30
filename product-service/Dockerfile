FROM openjdk:23

COPY ./target/product-service-0.0.1-SNAPSHOT.jar /app/product-service.jar
COPY ./application-prod.properties /app/application.properties

ENTRYPOINT ["java"]

CMD ["-jar", "/app/product-service.jar", "--spring.config.location=/app/application.properties"]