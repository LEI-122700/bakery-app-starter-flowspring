package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.entity.PickupLocation;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.PickupLocationRepository;

/**
 * Service class for managing {@link PickupLocation} entities.
 * Provides CRUD operations and filtering capabilities.
 */
@Service
public class PickupLocationService implements FilterableCrudService<PickupLocation> {

	private final PickupLocationRepository pickupLocationRepository;

	/**
	 * Creates a new {@code PickupLocationService}.
	 *
	 * @param pickupLocationRepository the repository used for managing pickup locations
	 */
	@Autowired
	public PickupLocationService(PickupLocationRepository pickupLocationRepository) {
		this.pickupLocationRepository = pickupLocationRepository;
	}

	/**
	 * Finds pickup locations matching the given filter.
	 *
	 * @param filter   optional filter string for location name
	 * @param pageable pagination information
	 * @return a page of matching pickup locations
	 */
	public Page<PickupLocation> findAnyMatching(Optional<String> filter, Pageable pageable) {
		if (filter.isPresent()) {
			String repositoryFilter = "%" + filter.get() + "%";
			return pickupLocationRepository.findByNameLikeIgnoreCase(repositoryFilter, pageable);
		} else {
			return pickupLocationRepository.findAll(pageable);
		}
	}

	/**
	 * Counts the number of pickup locations matching the given filter.
	 *
	 * @param filter optional filter string for location name
	 * @return the number of matching pickup locations
	 */
	public long countAnyMatching(Optional<String> filter) {
		if (filter.isPresent()) {
			String repositoryFilter = "%" + filter.get() + "%";
			return pickupLocationRepository.countByNameLikeIgnoreCase(repositoryFilter);
		} else {
			return pickupLocationRepository.count();
		}
	}

	/**
	 * Returns the default pickup location.
	 * <p>
	 * By default, it retrieves the first available pickup location.
	 *
	 * @return the default pickup location
	 */
	public PickupLocation getDefault() {
		return findAnyMatching(Optional.empty(), PageRequest.of(0, 1)).iterator().next();
	}

	/**
	 * Returns the repository used for managing pickup locations.
	 *
	 * @return the pickup location repository
	 */
	@Override
	public JpaRepository<PickupLocation, Long> getRepository() {
		return pickupLocationRepository;
	}

	/**
	 * Creates a new {@link PickupLocation}.
	 *
	 * @param currentUser the user creating the entity (not used in this implementation)
	 * @return a new pickup location instance
	 */
	@Override
	public PickupLocation createNew(User currentUser) {
		return new PickupLocation();
	}
}
