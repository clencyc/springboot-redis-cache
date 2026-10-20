package com.example.springboot_redis_cache.dto

import java.math.BigDecimal
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

data class ProductDTO(
    val id: Long? = null,
    @field:NotBlank(message = "Product name cannot be blank")
    val name: String = "",
    @field:Positive(message = "Price must be greater than zero")
    val price: BigDecimal = BigDecimal.ZERO,
)