---
name: spring-boot-integration-tests
description: '**WORKFLOW SKILL** — Generate integration tests for Spring Boot services with Cassandra. USE FOR: testing full stack (controllers → services → repositories), setting up embedded/test Cassandra, validating end-to-end flows. DO NOT USE FOR: unit tests or mocked dependency tests.'
---

# Spring Boot Integration Tests

## Overview
This skill guides creation of integration tests that validate the full stack of a Spring Boot microservice, including real database interactions with Cassandra. Integration tests verify that components work together correctly.

## Workflow Steps

1. **Set Up Test Environment**
   - Add Testcontainers dependency (for Cassandra container)
   - Or use embedded Cassandra for unit/integration tests
   - Configure test profiles in `application-test.yml`

2. **Create Test Database Setup**
   - Use `@Testcontainers` annotation for automatic container management
   - Initialize Cassandra keyspace and schema before tests
   - Load test fixtures or seedable data

3. **Configure Spring Test Context**
   - Use `@SpringBootTest` with embedded server
   - Load TestRestTemplate for HTTP testing
   - Wire in actual Spring beans (no mocks)

4. **Write Integration Test Methods**
   - Test full request → response flow
   - Use real database, not mocked repositories
   - Test error scenarios and edge cases

5. **Validate Database State**
   - Query Cassandra directly to verify data persistence
   - Check that transactions commit correctly
   - Validate cascading operations

6. **Clean Up Resources**
   - Use `@DirtiesContext` if needed to reset state
   - Leverage Testcontainers automatic cleanup
   - Handle container teardown properly

7. **Run and Verify**
   - Run with `mvn verify` or `mvn failsafe:integration-test`
   - Ensure tests are isolated and repeatable
   - Check logs for connection/schema issues

## Example: Service Integration Test

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ProductServiceIntegrationTest {

    @Container
    static CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:3.11.6");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProductRepository productRepository;

    @BeforeAll
    static void setupCassandra() {
        // Initialize keyspace and schema
        System.setProperty("cassandra.contact-point", cassandra.getContactPoint().getHostString());
    }

    @Test
    void shouldPersistAndRetrieveProduct() {
        // Given
        String productName = "TestProduct";
        String productId = UUID.randomUUID().toString();

        // When
        ResponseEntity<Product> response = restTemplate.postForEntity(
            "/api/products/add",
            new Product(productName, productId, "Test", BigDecimal.valueOf(99.99)),
            Product.class
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        Product retrieved = productRepository.findByNameAndId(productName, productId);
        assertThat(retrieved).isNotNull()
            .extracting(Product::getName, Product::getPrice)
            .containsExactly(productName, BigDecimal.valueOf(99.99));
    }
}
```

## Configuration

**application-test.yml:**
```yaml
spring:
  cassandra:
    contact-points: localhost:9042
    keyspace-name: betterbotz_test
    local-datacenter: dc1
```

## Quality Criteria
- Tests use real Cassandra, not mocks
- Tests are isolated and can run in any order
- Database is properly initialized and cleaned up
- Tests validate both happy path and error scenarios
- No hardcoded hostnames (use Testcontainers properties)

## Related Skills
- Use `spring-boot-unit-tests` for isolated component testing
- Use `cassandra-schema-design` for test schema validation</content>
