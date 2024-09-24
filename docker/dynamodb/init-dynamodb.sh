# Set the AWS CLI region to match DynamoDB Local
export AWS_REGION=us-west-2

if [ -z "${AWS_ACCESS_KEY_ID+x}" ]; then
    export AWS_ACCESS_KEY_ID="ACCESS"
fi
if [ -z "${AWS_SECRET_ACCESS_KEY+x}" ]; then
    export AWS_SECRET_ACCESS_KEY="ACCESS"
fi

aws configure set aws_access_key_id "$AWS_ACCESS_KEY_ID"
aws configure set aws_secret_access_key "$AWS_SECRET_ACCESS_KEY"
aws configure set region $AWS_REGION

sleep 1s

# Create a table named USERS
aws dynamodb create-table \
    --table-name USERS \
    --attribute-definitions AttributeName=PK,AttributeType=S AttributeName=SK,AttributeType=S \
    --key-schema AttributeName=PK,KeyType=HASH AttributeName=SK,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url http://localhost:8000

# Enable TTL for USERS Table
aws dynamodb update-time-to-live \
 --table-name USERS \
 --time-to-live-specification "Enabled=true, AttributeName=EXPIRES_AT" \
 --endpoint-url http://localhost:8000

# Table used to keep track of sessions
aws dynamodb create-table \
    --table-name AUTH_SESSIONS \
    --attribute-definitions AttributeName=SUBJECT,AttributeType=S AttributeName=SESSION_ID,AttributeType=S \
    --key-schema AttributeName=SUBJECT,KeyType=HASH AttributeName=SESSION_ID,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url http://localhost:8000

# Enable TTL for AUTH_SESSIONS Table
aws dynamodb update-time-to-live \
 --table-name AUTH_SESSIONS \
 --time-to-live-specification "Enabled=true, AttributeName=EXPIRES_AT" \
 --endpoint-url http://localhost:8000

# Don't let the container terminate
sleep infinity