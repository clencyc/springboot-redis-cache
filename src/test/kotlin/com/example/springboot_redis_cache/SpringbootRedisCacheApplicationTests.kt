package com.example.springboot_redis_cache

import com.fasterxml.jackson.databind.ObjectMapper
import com.example.springboot_redis_cache.dto.ProductDTO
import com.example.springboot_redis_cache.entity.Product
import com.example.springboot_redis_cache.repository.ProductRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureDataSourceInitialization
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.postgresql.PostgreSQLContainer
import java.math.BigDecimal

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureDataSourceInitialization
@AutoConfigureMockMvc
@Testcontainers
class SpringBootRedisCacheApplicationTests {

	companion object {
		@Container
		@ServiceConnection
		@JvmField
		val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7.4.2"))
			.withExposedPorts(6379)

		@Container
		@ServiceConnection
		val postgres = PostgreSQLContainer("postgres:16")

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
		productRepository.deleteAll()
	}

	@Test
	fun testCreateProductAndCacheIt() {
		val productDto = ProductDTO(id = null, name = "Laptop", price = BigDecimal.valueOf(1200L))

		val result = mockMvc.perform(
			post("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(productDto))
		)
			.andExpect(status().isCreated)
			.andReturn()

		val createdProduct = objectMapper.readValue(result.response.contentAsString, ProductDTO::class.java)
		val productId = createdProduct.id ?: throw AssertionError("Product ID should not be null")

		assertTrue(productRepository.findById(productId).isPresent)

		val cache = cacheManager.getCache("PRODUCT_CACHE")
		assertNotNull(cache)
		assertNotNull(cache?.get(productId, ProductDTO::class.java))
	}

	@Test
	fun testGetProductAndVerifyCache() {
		val product = Product(id = 0L, name = "Phone", price = BigDecimal.valueOf(800L))
		val savedProduct = productRepository.save(product)


		mockMvc.perform(get("/api/products/${savedProduct.id}"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.name").value("Phone"))

		Mockito.verify(productRepositorySpy, Mockito.times(1)).findById(savedProduct.id)
		Mockito.clearInvocations(productRepositorySpy)

		mockMvc.perform(get("/api/products/${savedProduct.id}"))
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.name").value("Phone"))

		Mockito.verify(productRepositorySpy, Mockito.times(0)).findById(savedProduct.id)
	}

	@Test
	fun testUpdateProductAndVerifyCache() {
		var product = Product(id = 0L, name = "Tablet", price = BigDecimal.valueOf(500L))
		product = productRepository.save(product)

		val updatedProductDto = ProductDTO(id = product.id, name = "Updated Tablet", price = BigDecimal.valueOf(550L))

		mockMvc.perform(
			put("/api/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updatedProductDto))
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.name").value("Updated Tablet"))
			.andExpect(jsonPath("$.price").value(550.0))

		val cache = cacheManager.getCache("PRODUCT_CACHE")
		assertNotNull(cache)
		val cachedProduct = cache?.get(product.id, ProductDTO::class.java)
		assertNotNull(cachedProduct)
		assertEquals("Updated Tablet", cachedProduct?.name)
	}

	@Test
	fun testDeleteProductAndEvictCache() {
		var product = Product(id = 0L, name = "Smartwatch", price = BigDecimal.valueOf(250L))
		product = productRepository.save(product)

		mockMvc.perform(delete("/api/products/${product.id}"))
			.andExpect(status().isNoContent)

		assertFalse(productRepository.findById(product.id).isPresent)

		val cache = cacheManager.getCache("PRODUCT_CACHE")
		assertNotNull(cache)
		assertNull(cache?.get(product.id))
	}
}
