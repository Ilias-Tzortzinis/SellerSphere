package com.sellersphere.productservice.rest;

import com.sellersphere.productservice.data.ProductCategory;
import com.sellersphere.productservice.data.ProductQuery;

import java.util.Map;

public interface ProductQueryFactory {

    ProductQuery create(ProductCategory category, Map<String, String> queryParams) throws InvalidProductQueryException;

}
