package com.streamcart.productservice;

import com.streamcart.productservice.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/products")
public final class ProductRestController {

    private final ProductsRepository service;
    private final ProductQueryParser productQueryParser;

    public ProductRestController(ProductsRepository service, ProductQueryParser productQueryParser) {
        this.service = service;
        this.productQueryParser = productQueryParser;
    }

    @GetMapping("/search/{category}")
    public List<ProductView> getProductsByCategory(@PathVariable String category,
                                                   @RequestParam MultiValueMap<String, String> query){
        Product.Category queryCategory = Product.Category.of(category)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown category: ".concat(category)));
        try {
            ProductQuery productQuery = productQueryParser.parse(queryCategory, query);
            return service.queryProductViews(queryCategory, productQuery);
        } catch (InvalidProductQueryException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad query", e);
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Product> findProductById(@PathVariable String productId){
        try {
            return service.findProductById(productId)
                    .map(ResponseEntity::ok)
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (InvalidProductIdException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ProductId");

        }
    }

}
