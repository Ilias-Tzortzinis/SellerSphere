package com.sellersphere.productservice.logic;

import com.sellersphere.productservice.data.Product;
import com.sellersphere.productservice.data.ProductView;
import com.sellersphere.productservice.data.ProductQuery;
import com.sellersphere.productservice.rest.InvalidProductQueryException;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<ProductView> searchProducts(ProductQuery query) throws InvalidProductQueryException;

    Optional<Product> findProductById(String productId);
}
