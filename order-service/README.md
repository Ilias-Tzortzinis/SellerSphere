# OrderService
The **Order Service** is responsible for the following:
- Place new order (reserve items stock) 
- View order history for a user

## Archirecture
```mermaid
graph LR
O[Order Service] <-->  D[DynamoDB]
O <--> M[MongoDB]
O -- OrderPlacedEvent --> S[Kafka]
```

## Operations
### Place Order
```mermaid
sequenceDiagram
actor U as User
participant S as OrderService
U->>S: POST /orders
S->>DynamoDB: Get user shopping cart
DynamoDB->>S: shopping cart
participant M as MongoDB
alt MongoDB Transaction
    S->>M: get required products
    M->>S: products
    S->>S: check products stock
    S->>M: reserve the required stock
    S->>Kafka: Push OrderPlaced message with delay
    S->>DynamoDB: store order
end
S->>U: OrderDetails
```

### View orders
```mermaid
sequenceDiagram
actor U as User
participant S as OrderService
participant D as DynamoDB
U->>S: GET /orders
S->>D: Get first 10 orders
D->>S: orders
S->>U: orders
U->>S: GET /orders?lastId=lastId
S->>D: Get 10 orders after lastId
D->>S: orders
S->>U: orders
```

## DynamoDB Tables
### Users Table
**PK (Partition Key):**
**OrderStatus:** PENDING_PAYMENT -> PAYMENT_IN_PROGRESS -> PAYMENT_COMPLETE / CANCELED

| PK                 | SK                   | OrderStatus     | PlacedAt    | TotalPrice | Items       |
|--------------------|----------------------|-----------------|-------------|------------|-------------|
| USER#bob@bmail.com | ORDER#20240917123456 | PENDING_PAYMENT | {UnixEpoch} | 500        | OrderItem[] |
## API
```
GET /orders?lastId&year&month&day
Authorization: user
```

```
POST /orders
Authorization: user
```