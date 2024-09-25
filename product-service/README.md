# Product Service

## API

```
GET /products/{category}
{ productQuery }

MongoDB.products.findMany( encode(productQuery) )
Kafka.topic("category_view").emit(CategoryView(category, productQuery, Auth ? Auth.userId : null))

return []{ productId, name, quantity, price, description, image }
```

```
GET /products/{productId}

MongoDB.products.find( _id = productId )
Kafka.topic("product_view").emit(ProductView(productId, Auth ? Auth.userId : null))

return { seller, name, quantity, price, description, images }
```

```
GET /products/search
{ searchText }

MongoDB.products.searchTextIndex.findMany( searchText ).limit(5)

return []{ productId, name, quantity, price, description, image }
```

```
POST /products
Authorization: Seller || IT
{ name, quantity, price, description, tags, images }

MongoDB.products.insert( ... )
```

```
PATCH /products/{productId}
Authorization: Seller || IT
{ name, quantity, price, description, tags, images }

MongoDB.products.updateOne( ... )
```

```
GET /products/recommendation
    Authorization: USER

```