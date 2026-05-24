package com.datastax.examples.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class ProductResponseDto {

    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Instant lastUpdated;

    public ProductResponseDto() {
    }

    public ProductResponseDto(UUID id, String name, String description, BigDecimal price, Instant lastUpdated) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.lastUpdated = lastUpdated;
    }

    public static ProductResponseDto fromEntity(Product product) {
        return new ProductResponseDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getLastUpdated()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
