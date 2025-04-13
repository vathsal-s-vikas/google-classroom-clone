package com.classroom.clone.repository;

import com.classroom.clone.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    Optional<User> findByOauthProviderAndOauthId(User.OAuthProvider provider, String oauthId);

    boolean existsByEmail(String email);
}