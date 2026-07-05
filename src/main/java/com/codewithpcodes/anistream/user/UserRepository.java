package com.codewithpcodes.anistream.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    @Query(value = "select u from User u " +
            "where u.username like %:query% " +
            "and u.type = 'REGISTERED' " +
            "and u.id != :currentUserId")
    List<User> searchByUsername(
            @Param("query") String query,
            @Param("currentUserId") UUID currentUserId
    );

}
