package com.example.springboot_redis_cache


import com.example.springboot_redis_cache.dto.ProductDTO
import com.example.springboot_redis_cache.entity.Product
import com.example.springboot_redis_cache.repository.ProductRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.service.registry.ImportHttpServices
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.junit.jupiter.Container
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class SpringBootRedisCacheApplicationTests {

	companion object {
		// Kotlin uses companion objects to declare fields equivalent to Java's `static` variables
		@Container
		@ServiceConnection
		@JvmField
		val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7.4.2"))
			.withExposedPorts(6379)
	}

	@Autowired
	private lateinit var mockMvc: MockMvc

	@Autowired
	private lateinit var productRepository: ProductRepository

	@Autowired
	private lateinit var cacheManager: CacheManager

	@MockitoSpyBean
	private lateinit var productRepositorySpy: ProductRepository

	private val objectMapper = ObjectMapper()

	@BeforeEach
	fun setUp() {
		productRepository.deleteAll() // Clean slate setup
	}

	@Test
	fun testCreateProductAndCacheIt() {
		val productDto = ProductDTO(id = null, name = "Laptop", price = BigDecimal.valueOf(1200L))

		// Step 1: Create a Product via REST call
		val result = mockMvc.perform(
			post("/api/products") // Changed to match your Kotlin controller route mapping
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(productDto))
		)
			.andExpect(status().isCreated)
			.andReturn()

		val createdProduct = objectMapper.readValue(result.response.contentAsString, ProductDTO::class.java)
		val productId = createdProduct.id ?: throw AssertionError("Product ID should not be null")

		// Step 2: Check database existence state
		assertTrue(productRepository.findById(productId).isPresent)

		// Step 3: Check Redis Caching layer storage metrics
		// In Kotlin, use 'PRODUCT_CACHE' directly if defined in a companion object/file-level string,
		// or swap it with a raw string value like "PRODUCT_CACHE"
		val cache = cacheManager.getCache("PRODUCT_CACHE")
		assertNotNull(cache)
		assertNotNull(cache?.get(productId, ProductDTO::class.java))
	}

	@Test
	fun testGetProductAndVerifyCache() {
		// Step 1: Initialize Database seed item
		val product = Product(
			id = 0L,
			name = "Phone",
			price = BigDecimal.valueOf(800L)
		)
		val savedProduct = productRepository.save(product)

		// Step 2: First invocation: DB query hit
		mockMvc.perform(get("/api/products/\${savedProduct.id}"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("\$.name").value("Phone"))

		Mockito.verify(productRepositorySpy, Mockito.times(1)).findById(savedProduct.id)
		Mockito.clearInvocations(productRepositorySpy)

		// Step 3: Second invocation: Cached Redis intercept hit (0 DB touches)
		mockMvc.perform(get("/api/products/\${savedProduct.id}"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("\$.name").value("Phone"))

		Mockito.verify(productRepositorySpy, Mockito.times(0)).findById(savedProduct.id)
	}

	@Test
	fun testUpdateProductAndVerifyCache() {
		// Step 1: Seed Initial Product Instance
		var product = Product(id = 0L, name = "Tablet", price = BigDecimal.valueOf(500L))
		product = productRepository.save(product)

		val updatedProductDto = ProductDTO(id = product.id, name = "Updated Tablet", price = BigDecimal.valueOf(550L))

		// Step 2: Push update via REST endpoint mutation
		mockMvc.perform(
			put("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updatedProductDto))
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("\$.name").value("Updated Tablet"))
			.andExpect(jsonPath("\$.price").value(550.0))

		// Step 3: Confirm Cache is updated immediately
		val cache = cacheManager.getCache("PRODUCT_CACHE")
		assertNotNull(cache)
		val cachedProduct = cache?.get(product.id, ProductDTO::class.java)
		assertNotNull(cachedProduct)
		assertEquals("Updated Tablet", cachedProduct?.name)
	}

	@Test
	fun testDeleteProductAndEvictCache() {
		// Step 1: Seed Target Entity
		var product = Product(id = 0L, name = "Smartwatch", price = BigDecimal.valueOf(250L))
		product = productRepository.save(product)

		// Step 2: Dispatch delete instruction pipeline
		mockMvc.perform(delete("/api/products/\${product.id}"))
			.andExpect(status().isNoContent)

		// Step 3: Verify execution dropped database entry
		assertFalse(productRepository.findById(product.id).isPresent)

		// Step 4: Verify Cache Eviction occurred safely
		val cache = cacheManager.getCache("PRODUCT_CACHE")
		assertNotNull(cache)
		assertNull(cache?.get(product.id))
	}
}
