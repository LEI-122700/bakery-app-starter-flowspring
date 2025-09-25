package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.entity.Product;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.ProductRepository;

/**
 * Service class for managing {@link Product} entities.
 * <p>
 * Provides CRUD operations with filtering, validation, and uniqueness checks
 * for product names.
 */
@Service
public class ProductService implements FilterableCrudService<Product> {

    /** Repository for accessing {@link Product} data. */
    private final ProductRepository productRepository;

    /**
     * Creates a new {@link ProductService} with the given {@link ProductRepository}.
     *
     * @param productRepository the product repository
     */
    @Autowired
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Finds products matching the given filter by name.
     *
     * @param filter   optional filter text
     * @param pageable pagination information
     * @return a page of matching products
     */
    @Override
    public Page<Product> findAnyMatching(Optional<String> filter, Pageable pageable) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return productRepository.findByNameLikeIgnoreCase(repositoryFilter, pageable);
        } else {
            return find(pageable);
        }
    }

    /**
     * Counts the number of products matching the given filter by name.
     *
     * @param filter optional filter text
     * @return the number of matching products
     */
    @Override
    public long countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return productRepository.countByNameLikeIgnoreCase(repositoryFilter);
        } else {
            return count();
        }
    }

    /**
     * Finds all products with pagination.
     *
     * @param pageable pagination information
     * @return a page of products
     */
    public Page<Product> find(Pageable pageable) {
        return productRepository.findBy(pageable);
    }

    /**
     * Returns the repository associated with this service.
     *
     * @return the {@link ProductRepository}
     */
    @Override
    public JpaRepository<Product, Long> getRepository() {
        return productRepository;
    }

    /**
     * Creates a new {@link Product} instance.
     *
     * @param currentUser the user performing the action
     * @return a new product instance
     */
    @Override
    public Product createNew(User currentUser) {
        return new Product();
    }

    /**
     * Saves a product entity while validating name uniqueness.
     * <p>
     * If another product with the same name already exists, a
     * {@link UserFriendlyDataException} is thrown.
     *
     * @param currentUser the user performing the action
     * @param entity      the product entity to save
     * @return the saved product
     * @throws UserFriendlyDataException if a product with the same name already exists
     */
    @Override
    public Product save(User currentUser, Product entity) {
        try {
            return FilterableCrudService.super.save(currentUser, entity);
        } catch (DataIntegrityViolationException e) {
            throw new UserFriendlyDataException(
                    "There is already a product with that name. Please select a unique name for the product.");
        }
    }
}
