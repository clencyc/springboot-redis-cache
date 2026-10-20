package com.example.springboot_redis_cache.entity

import java.math.BigDecimal


data class Product (
    val id: Long,  // getter method automatic declaration
    var name: String? = null,
    var price: BigDecimal? = null,
)