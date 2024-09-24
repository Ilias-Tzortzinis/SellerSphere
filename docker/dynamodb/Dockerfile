FROM amazonlinux:latest
RUN yum update
# Install the required libraries for DynamoDB
RUN yum install -y java-21-amazon-corretto-headless aws-cli tar gzip

# Setup DynamoDB Local
RUN mkdir /dynamodb_local
WORKDIR /dynamodb_local
RUN curl -O https://d1ni2b6xgvw0s0.cloudfront.net/v2.x/dynamodb_local_latest.tar.gz && \
    tar -zxvf dynamodb_local_latest.tar.gz

# Copy initilization script
COPY ./init-dynamodb.sh ./init-dynamodb.sh
# Make the initialization script executable
RUN chmod +x init-dynamodb.sh

EXPOSE 8000
# Start DynamoDB Local and run the initialization script
ENTRYPOINT ["/bin/sh", "-c", "java -Djava.library.path=./DynamoDBLocal_lib -jar DynamoDBLocal.jar -sharedDb & ./init-dynamodb.sh"]