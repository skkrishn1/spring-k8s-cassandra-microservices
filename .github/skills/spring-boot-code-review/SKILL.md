---
name: spring-boot-code-review
description: '**CHECKLIST SKILL** — Review Spring Boot microservice code for best practices, security, and maintainability. USE FOR: pre-commit checks, pull request reviews, ensuring consistency. DO NOT USE FOR: generating code or runtime debugging.'
---

# Spring Boot Code Review Checklist

## Overview
This skill provides a comprehensive checklist for reviewing Spring Boot microservices code, particularly those using Cassandra. Use this to ensure code quality, security, and adherence to best practices before merging.

## Checklist Categories

### Architecture & Design
- [ ] **Layer Separation**: Controllers handle HTTP, Services contain business logic, Repositories manage data access
- [ ] **Dependency Injection**: Use constructor injection over field injection
- [ ] **SOLID Principles**: Single responsibility, open/closed, etc.
- [ ] **Microservice Boundaries**: No tight coupling between services

### Spring Boot Specific
- [ ] **Proper Annotations**: Correct use of @RestController, @Service, @Repository, @Component
- [ ] **Configuration**: Externalize config via @ConfigurationProperties or @Value
- [ ] **Profiles**: Appropriate use of @Profile for different environments
- [ ] **Actuator**: Health checks, metrics, and management endpoints configured

### Cassandra Integration
- [ ] **Keyspace Management**: Proper keyspace creation and configuration
- [ ] **Schema Design**: Appropriate use of partitions and clustering keys
- [ ] **Prepared Statements**: Use prepared statements for performance
- [ ] **Connection Handling**: Proper session management and error handling

### Security
- [ ] **Input Validation**: Use Bean Validation (@Valid) on request bodies
- [ ] **Authentication/Authorization**: Secure endpoints with Spring Security if needed
- [ ] **CORS**: Properly configured for cross-origin requests
- [ ] **Sensitive Data**: No hardcoded secrets or credentials

### Error Handling & Logging
- [ ] **Exception Handling**: Global exception handlers with @ControllerAdvice
- [ ] **HTTP Status Codes**: Appropriate status codes for different scenarios
- [ ] **Logging**: Use SLF4J with appropriate log levels
- [ ] **Error Messages**: User-friendly error responses without sensitive info

### Testing
- [ ] **Unit Tests**: Cover business logic with mocked dependencies
- [ ] **Integration Tests**: Test with real Cassandra when appropriate
- [ ] **Test Coverage**: Aim for >80% coverage
- [ ] **Test Naming**: Descriptive test method names

### Performance & Best Practices
- [ ] **Pagination**: Implement for large result sets
- [ ] **Caching**: Use @Cacheable where appropriate
- [ ] **Async Processing**: Use @Async for non-blocking operations
- [ ] **Resource Management**: Proper cleanup of resources

### Code Quality
- [ ] **Code Style**: Follow project conventions (checkstyle, spotless)
- [ ] **Documentation**: Javadoc for public APIs
- [ ] **Naming Conventions**: Consistent naming for classes, methods, variables
- [ ] **DRY Principle**: No code duplication

## Usage
- Check off each item during review
- Note any violations with specific line numbers
- Suggest improvements or alternatives
- Ensure all critical items are addressed before approval

## Related Skills
- Use `spring-boot-unit-tests` skill for generating missing tests
- Integration with CI/CD pipelines for automated checks