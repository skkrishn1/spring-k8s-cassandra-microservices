package com.datastax.examples.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@Tag(name = "Products", description = "Product API endpoints")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("products/search/{name}")
    @Operation(summary = "Find products by name", description = "Search for products by their name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDto.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Iterable<ProductResponseDto>> findProductsByName(@PathVariable String name) {
        Iterable<Product> products = productService.find(name);
        List<ProductResponseDto> dtos = new ArrayList<>();
        products.forEach(p -> dtos.add(ProductResponseDto.fromEntity(p)));
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("products/search/{name}/{id}")
    @Operation(summary = "Find product by name and ID", description = "Search for a specific product by name and UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product found",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Product not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProductResponseDto> findProductsByNameAndId(@PathVariable String name, @PathVariable UUID id) {
        Product product = productService.find(name, id);
        if (product == null) {
            throw new ResourceNotFoundException("Product not found: name=" + name + ", id=" + id);
        }
        return ResponseEntity.ok(ProductResponseDto.fromEntity(product));
    }

    @PostMapping("products/add")
    @Operation(summary = "Add a new product", description = "Create a new product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Product created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ProductResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ProductResponseDto> addProduct(@Valid @RequestBody ProductRequestDto requestDto){
        Product product = new Product(
                requestDto.getName(),
                requestDto.getId(),
                requestDto.getDescription(),
                requestDto.getPrice(),
                Instant.now()
        );
        productService.add(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductResponseDto.fromEntity(product));
    }

    @DeleteMapping("products/delete/{name}")
    @Operation(summary = "Delete products by name", description = "Delete all products with the given name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products deleted"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> removeProductByName(@PathVariable String name){
        productService.remove(name);
        return ResponseEntity.ok(name);

    }

    @DeleteMapping("products/delete/{name}/{id}")
    @Operation(summary = "Delete product by name and ID", description = "Delete a specific product by name and UUID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product deleted"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<String> removeProductByNameAndId(@PathVariable String name, @PathVariable UUID id){
        productService.remove(name, id);
        return ResponseEntity.ok(name + "," + id);
    }

}
