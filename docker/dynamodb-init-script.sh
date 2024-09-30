        aws dynamodb create-table \
        --table-name Users \
        --attribute-definitions AttributeName=PK,AttributeType=S  AttributeName=SK,AttributeType=S \
        --key-schema AttributeName=PK,KeyType=HASH AttributeName=SK,KeyType=RANGE \
        --billing-mode PAY_PER_REQUEST \
        --endpoint-url http://dynamodb:8000 \

        aws dynamodb create-table \
        --table-name AuthSessions \
        --attribute-definitions AttributeName=Subject,AttributeType=S AttributeName=SessionId,AttributeType=S \
        --key-schema AttributeName=Subject,KeyType=HASH AttributeName=SessionId,KeyType=RANGE \
        --billing-mode PAY_PER_REQUEST \
        --endpoint-url http://dynamodb:8000