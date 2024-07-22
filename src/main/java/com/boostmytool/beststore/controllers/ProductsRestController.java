package com.boostmytool.beststore.controllers;

import java.nio.file.Path;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.boostmytool.beststore.models.Product;
import com.boostmytool.beststore.models.ProductDto;
import com.boostmytool.beststore.services.ProductsRepository;

import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products") // Changed to avoid conflict with the traditional view controller
public class ProductsRestController {

    @Autowired
    private ProductsRepository repo;

    // Get all products
    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts() {
        List<Product> products = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    // Get a product by ID
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable int id) {
        Optional<Product> product = repo.findById(id);
        return product.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                      .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // Create a new product
    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @ModelAttribute ProductDto productDto) {
        try {
            MultipartFile image = productDto.getImageFile();
            Date createdAt = new Date();
            String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }

            Product product = new Product();
            product.setName(productDto.getName());
            product.setBrand(productDto.getBrand());
            product.setCategory(productDto.getCategory());
            product.setPrice(productDto.getPrice());
            product.setDescription(productDto.getDescription());
            product.setCreatedAt(new java.sql.Date(createdAt.getTime()));
            product.setImageFileName(storageFileName);

            repo.save(product);
            return new ResponseEntity<>(product, HttpStatus.CREATED);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // Update a product
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable int id, @Valid @ModelAttribute ProductDto productDto) {
        Optional<Product> optionalProduct = repo.findById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();

            try {
                if (!productDto.getImageFile().isEmpty()) {
                    String uploadDir = "public/images/";
                    Path oldImagePath = Paths.get(uploadDir + product.getImageFileName());

                    try {
                        Files.delete(oldImagePath);
                    } catch (Exception ex) {
                        System.out.println("Exception: " + ex.getMessage());
                    }

                    MultipartFile image = productDto.getImageFile();
                    Date createdAt = new Date();
                    String storageFileName = createdAt.getTime() + "_" + image.getOriginalFilename();

                    try (InputStream inputStream = image.getInputStream()) {
                        Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
                    }

                    product.setImageFileName(storageFileName);
                }

                product.setName(productDto.getName());
                product.setBrand(productDto.getBrand());
                product.setCategory(productDto.getCategory());
                product.setPrice(productDto.getPrice());
                product.setDescription(productDto.getDescription());

                repo.save(product);
                return new ResponseEntity<>(product, HttpStatus.OK);

            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // Delete a product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable int id) {
        Optional<Product> optionalProduct = repo.findById(id);
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();

            Path imagePath = Paths.get("public/images/" + product.getImageFileName());

            try {
                Files.delete(imagePath);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }

            repo.delete(product);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
