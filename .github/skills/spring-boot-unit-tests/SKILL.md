---
name: spring-boot-unit-tests
description: '**WORKFLOW SKILL** — Generate unit tests for Spring Boot components. USE FOR: creating test classes for controllers, services, repositories; setting up mocks; writing assertions. DO NOT USE FOR: integration tests or non-Java code.'
---

# Spring Boot Unit Test Generation

## Overview
This skill guides the creation of comprehensive unit tests for Spring Boot microservices, focusing on controllers, services, and repositories. It follows best practices for mocking dependencies and writing meaningful assertions.

## Workflow Steps

1. **Identify Target Component**
   - Analyze the class to test (e.g., ProductController, ProductService)
   - Review public methods and their dependencies

2. **Create Test Structure**
   - Create test class in `src/test/java` with matching package
   - Name: `{ClassName}Test.java`
   - Add necessary imports (JUnit 5, Spring Test, Mockito)

3. **Configure Test Context**
   - For Controllers: Use `@WebMvcTest` with MockMvc
   - For Services: Use `@SpringBootTest` or `@ExtendWith(MockitoExtension.class)`
   - For Repositories: Use `@DataCassandraTest` or similar

4. **Mock Dependencies**
   - Use `@MockBean` for Spring-managed beans
   - Use `@Mock` for other dependencies
   - Inject mocks into the test subject

5. **Write Test Methods**
   - One test per scenario (happy path, error cases, edge cases)
   - Use descriptive names: `shouldReturnProductWhenFound()`
   - Test both success and failure paths

6. **Add Assertions**
   - Verify return values, exceptions, and side effects
   - Use AssertJ for fluent assertions
   - Check HTTP status codes for controller tests

7. **Validate and Refactor**
   - Run tests with `mvn test`
   - Ensure high code coverage
   - Refactor tests for readability and maintainability

## Example: Controller Test

```java
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Test
    void shouldReturnProductWhenFound() throws Exception {
        // Given
        Product product = new Product();
        when(productService.findById(anyString())).thenReturn(Optional.of(product));

        // When & Then
        mockMvc.perform(get("/api/products/{id}", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Product"));
    }
}
```

## Quality Criteria
- Tests are isolated and fast
- Mock external dependencies (Cassandra, other services)
- Cover error scenarios and validation
- Use meaningful test data

## Related Skills
- Consider creating a separate skill for code review checklists
- Integration with CI/CD for automated test execution