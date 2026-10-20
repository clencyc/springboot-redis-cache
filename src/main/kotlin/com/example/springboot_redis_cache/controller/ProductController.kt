package com.example.springboot_redis_cache.controller

import com.example.springboot_redis_cache.dto.ProductDTO
import com.example.springboot_redis_cache.entity.Product
import com.example.springboot_redis_cache.service.ProductService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/products")
class ProductController (
    private val productService: ProductService
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProduct(@RequestBody productDto: ProductDTO): ProductDTO {
        return productService.createProduct(productDto)
    }


    @GetMapping("/{productId}")
    @ResponseStatus(HttpStatus.OK)
    fun getProduct(@PathVariable productId: Long): ProductDTO {
        return productService.getProduct(productId)
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    fun updateProduct(@RequestBody productDto: ProductDTO): ProductDTO {
        return productService.updateProduct(productDto)
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteProduct(@PathVariable productId: Long) {
        productService.deleteProduct(productId)
    }
}

// Adding and updating product -> add something to cache
// deleteproduct -> delete also from cache
// getproduct -> fetch from the cache