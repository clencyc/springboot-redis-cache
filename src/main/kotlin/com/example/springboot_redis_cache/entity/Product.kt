package com.example.springboot_redis_cache.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "product")
data class Product (
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long,  // getter method automatic declaration
    var name: String? = null,
    var price: BigDecimal? = null,
)