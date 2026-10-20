package com.example.springboot_redis_cache.repository

import com.example.springboot_redis_cache.entity.Product
import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<Product, Long>

