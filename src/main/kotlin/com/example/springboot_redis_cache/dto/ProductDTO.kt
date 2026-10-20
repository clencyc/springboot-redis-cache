package com.example.springboot_redis_cache.dto

import java.math.BigDecimal

data class ProductDTO(
    val id: Long?,

    @field:NotBlank(message = "Product name cannot be blank")
    val name: String,

    @field:Positive(message = "Price must be greater than zero")
    val price: BigDecimal,
)