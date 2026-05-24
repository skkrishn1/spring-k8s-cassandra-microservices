package com.datastax.examples.product;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public class ProductRequestDto {

    @NotBlank(message = "Product name cannot be blank")
    private String name;

    @NotNull(message = "Product ID cannot be null")
    private UUID id;

    @NotBlank(message = "Product description cannot be blank")
    private String description;

    @NotNull(message = "Product price cannot be null")
    @Positive(message = "Product price must be positive")
    private BigDecimal price;

    public ProductRequestDto() {
    }

    public ProductRequestDto(String name, UUID id, String description, BigDecimal price) {
        this.name = name;
        this.id = id;
        this.description = description;
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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
}
