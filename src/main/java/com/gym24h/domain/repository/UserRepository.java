package com.gym24h.domain.repository;

import com.gym24h.domain.model.user.User;
import com.gym24h.domain.model.user.UserId;

import java.util.Optional;

public interface UserRepository {

    Optional<User> findById(UserId userId);

    User save(User user);
}
