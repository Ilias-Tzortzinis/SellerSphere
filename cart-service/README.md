# Cart Service

The cart service manages the cart of the users.

## DynamoDB Tables used
### USERS Table

**PK (Partition Key):** The same used in [UserService](../user-service/README.md).

**SK (Sort Key):** The sort key used for cart items is CART_ITEM#{productId}

**NAME:** The name of the product (to avoid joins).

**QUANTITY:** The quantity of that product selected by the user.

| PK                 | SK             | NAME  | QUANTITY |
|--------------------|----------------|-------|----------|
| USER#bob@bmail.com | CART_ITEM#4214 | Apple | 50       |

## API
### Get user cart
```
GET /cart
Authorization: USER

DynamoDB.query("USERS").key("PK", Auth.userId)
                        .where("begins_with(SK, 'CART_ITEM#')")
                        .projectionExp("SK, NAME, QUANTITY")
```

### Update user cart (add/edit/remove item)
```
PATCH /cart
Authorization: USER
{ productId, name, quantity }

if quantity == 0:
    DynamoDB.delete("USERS").key("PK", Auth.userId, "SK", "CART_ITEM#" + productId)
else:
    DynamoDB.update("USERS").key("PK", Auth.userId, "SK", "CART_ITEM#" + productId)
                        .updateExp("SET NAME = :name, QUANTITY = :quantity")
```
