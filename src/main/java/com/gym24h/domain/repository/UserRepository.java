package com.gym24h.domain.repository;

import com.gym24h.domain.model.user.User;
import com.gym24h.domain.model.user.UserId;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(UserId userId);

    Optional<User> findByLineUserId(String lineUserId);

    List<User> findAll();

    User save(User user);
}
