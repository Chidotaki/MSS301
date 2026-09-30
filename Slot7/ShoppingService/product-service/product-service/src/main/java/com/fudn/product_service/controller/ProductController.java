package com.fudn.product_service.controller;

import com.fudn.product_service.dto.ProductRequest;
import com.fudn.product_service.dto.ProductResponse;
import com.fudn.product_service.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // ==========================================================
    // ĐÃ ĐƯỢC IMPLEMENT — đọc kỹ làm mẫu
    // ==========================================================

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(@RequestBody ProductRequest productRequest) {
        return productService.createProduct(productRequest);
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    // ==========================================================
    // TODO 3 — Endpoint UPDATE sản phẩm
    // ==========================================================
    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable String id,
            @RequestBody ProductRequest productRequest) {

        return productService.updateProduct(id, productRequest);
    }

    // ==========================================================
    // TODO 4 — Endpoint DELETE sản phẩm
    // ==========================================================
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
    }
}
