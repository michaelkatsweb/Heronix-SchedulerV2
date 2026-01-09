package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.User;
import com.heronix.scheduler.repository.UserRepository;
import com.heronix.scheduler.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * User Service Implementation
 * Provides user management operations for the scheduler
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public List<User> findAll() {
        log.debug("Finding all users");
        return userRepository.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        log.debug("Finding user by id: {}", id);
        return userRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        return userRepository.findByUsername(username);
    }

    @Override
    public List<User> findAllEnabled() {
        log.debug("Finding all enabled users");
        return userRepository.findByEnabledTrue();
    }

    @Override
    public List<User> findAllAdministrators() {
        log.debug("Finding all administrators");
        return userRepository.findAllAdministrators();
    }
}
