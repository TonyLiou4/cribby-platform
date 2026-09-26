package com.santosh.roommate_marketplace_api.favorite.repository;

import com.santosh.roommate_marketplace_api.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    Optional<Favorite> findByUserIdAndListingId(Long userId, Long listingId);

    List<Favorite> findByUserIdOrderByCreatedAtDesc(Long userId);
}
