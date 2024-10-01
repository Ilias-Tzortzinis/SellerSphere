FROM openjdk:23
COPY ./target/order-service-0.0.1-SNAPSHOT.jar /app/order-service.jar
COPY ./application-prod.properties /app/application.properties
ENTRYPOINT ["java"]
CMD ["-jar", "/app/order-service.jar", "--spring.config.location=/app/application.properties"]