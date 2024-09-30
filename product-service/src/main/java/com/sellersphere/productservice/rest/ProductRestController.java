package com.sellersphere.productservice.rest;

import com.sellersphere.productservice.data.Product;
import com.sellersphere.productservice.data.ProductCategory;
import com.sellersphere.productservice.data.ProductOverview;
import com.sellersphere.productservice.data.ProductQuery;
import com.sellersphere.productservice.logic.ProductService;
import org.hibernate.validator.constraints.Length;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
public final class ProductRestController {

    private final ProductService productService;
    private final ProductQueryFactory queryFactory;

    public ProductRestController(ProductService productService, ProductQueryFactory queryFactory) {
        this.productService = productService;
        this.queryFactory = queryFactory;
    }

    @GetMapping("/search/{category}")
    public List<ProductOverview> searchProducts(@PathVariable String category,
                                                @RequestParam Map<String, String> queryParams){
        ProductCategory productCategory = ProductCategory.fromString(category)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown category: ".concat(category)));
        try {
            ProductQuery productQuery = queryFactory.create(productCategory, queryParams);
            return productService.searchProducts(productQuery);
        } catch (InvalidProductQueryException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, null, e);
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> findProductById(@PathVariable @Length(min = 24, max = 24) String productId){
        return productService.findProductById(productId).map(ResponseEntity::ok)
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


}
