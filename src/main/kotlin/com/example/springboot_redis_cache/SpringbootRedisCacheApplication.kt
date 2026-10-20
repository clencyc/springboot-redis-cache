package com.example.springboot_redis_cache

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication
@EnableCaching   // helps enable caching
class SpringbootRedisCacheApplication

fun main(args: Array<String>) {
	runApplication<SpringbootRedisCacheApplication>(*args)
}
