# StreamCart Arhirecture

StreamCart must be able to handle a million of users and products 
with high availability, fault-tolerance (no order must be lost) and low latency.

## High Level Overview
```mermaid
graph LR
Users --> G[Gateway]
Sellers --> G

G --> M[Micro services]
M -- user data, orders, products --> Databases
M -- realtime events --> P[Stateful Processing]
P -- recommendations --> Databases
```

### Micro Services
- User Service (User Management)
- Cart Service (Cart Management)
- Product Service (Product Management)
- Order Service (Order Management)

### Databases
- DynamoDB (Users, Sellers, AuthSessions, Carts, Orders)
- MongoDB (Products)
- S3 (Product Images)

### Real time Processing
- Apache Kafka
- Apache Flink