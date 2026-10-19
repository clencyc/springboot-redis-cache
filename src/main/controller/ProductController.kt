package com.example.springboot_redis_cache.controller

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/products")
class ProductController (
    private val productService: ProductService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@RequestBody product: Product): Product {
        return productService.createProduct(product)
    }

    @GetMapping("/{productId}")
    @ResponseStatus(HttpStatus.OK)
    fun getProduct(@PathVariable productId: Long): Product {
        return productService.getProduct(productId)
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    fun updateProduct(product: Product): Product {
        return productService.updateProduct(product)
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(@PathVariable productId: Long) {
        return productService.deleteProduct(productId)
    }

}