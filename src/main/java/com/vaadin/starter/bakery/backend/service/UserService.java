package com.vaadin.starter.bakery.backend.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.UserRepository;

/**
 * Service class for managing {@link User} entities.
 * <p>
 * Provides CRUD operations with additional validation such as preventing
 * deletion of the current logged-in user or modification of locked accounts.
 */
@Service
public class UserService implements FilterableCrudService<User> {

    /** Exception message when trying to modify or delete a locked user. */
    public static final String MODIFY_LOCKED_USER_NOT_PERMITTED = "User has been locked and cannot be modified or deleted";

    /** Exception message when trying to delete own account. */
    private static final String DELETING_SELF_NOT_PERMITTED = "You cannot delete your own account";

    /** Repository for accessing {@link User} data. */
    private final UserRepository userRepository;

    /**
     * Creates a new {@link UserService} with the given {@link UserRepository}.
     *
     * @param userRepository the user repository
     */
    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds users matching the given filter across email, first name, last name, or role.
     *
     * @param filter   optional filter text
     * @param pageable pagination information
     * @return a page of matching users
     */
    public Page<User> findAnyMatching(Optional<String> filter, Pageable pageable) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return getRepository()
                    .findByEmailLikeIgnoreCaseOrFirstNameLikeIgnoreCaseOrLastNameLikeIgnoreCaseOrRoleLikeIgnoreCase(
                            repositoryFilter, repositoryFilter, repositoryFilter, repositoryFilter, pageable);
        } else {
            return find(pageable);
        }
    }

    /**
     * Counts the number of users matching the given filter.
     *
     * @param filter optional filter text
     * @return the number of matching users
     */
    @Override
    public long countAnyMatching(Optional<String> filter) {
        if (filter.isPresent()) {
            String repositoryFilter = "%" + filter.get() + "%";
            return userRepository.countByEmailLikeIgnoreCaseOrFirstNameLikeIgnoreCaseOrLastNameLikeIgnoreCaseOrRoleLikeIgnoreCase(
                    repositoryFilter, repositoryFilter, repositoryFilter, repositoryFilter);
        } else {
            return count();
        }
    }

    /**
     * Returns the repository associated with this service.
     *
     * @return the {@link UserRepository}
     */
    @Override
    public UserRepository getRepository() {
        return userRepository;
    }

    /**
     * Finds all users with pagination.
     *
     * @param pageable pagination information
     * @return a page of users
     */
    public Page<User> find(Pageable pageable) {
        return getRepository().findBy(pageable);
    }

    /**
     * Saves a user entity after validating locked state.
     *
     * @param currentUser the user performing the action
     * @param entity      the user entity to save
     * @return the saved user
     * @throws UserFriendlyDataException if the user is locked
     */
    @Override
    public User save(User currentUser, User entity) {
        throwIfUserLocked(entity);
        return getRepository().saveAndFlush(entity);
    }

    /**
     * Deletes a user after validating self-deletion and locked state.
     *
     * @param currentUser  the user performing the action
     * @param userToDelete the user to be deleted
     * @throws UserFriendlyDataException if attempting to delete own account or a locked user
     */
    @Override
    @Transactional
    public void delete(User currentUser, User userToDelete) {
        throwIfDeletingSelf(currentUser, userToDelete);
        throwIfUserLocked(userToDelete);
        FilterableCrudService.super.delete(currentUser, userToDelete);
    }

    /**
     * Throws an exception if the current user attempts to delete themselves.
     *
     * @param currentUser the user performing the action
     * @param user        the user to delete
     * @throws UserFriendlyDataException if attempting to delete own account
     */
    private void throwIfDeletingSelf(User currentUser, User user) {
        if (currentUser.equals(user)) {
            throw new UserFriendlyDataException(DELETING_SELF_NOT_PERMITTED);
        }
    }

    /**
     * Throws an exception if the user entity is locked.
     *
     * @param entity the user to validate
     * @throws UserFriendlyDataException if the user is locked
     */
    private void throwIfUserLocked(User entity) {
        if (entity != null && entity.isLocked()) {
            throw new UserFriendlyDataException(MODIFY_LOCKED_USER_NOT_PERMITTED);
        }
    }

    /**
     * Creates a new {@link User} instance.
     *
     * @param currentUser the user performing the action
     * @return a new user instance
     */
    @Override
    public User createNew(User currentUser) {
        return new User();
    }
}
