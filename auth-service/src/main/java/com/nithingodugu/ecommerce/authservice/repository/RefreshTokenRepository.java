package com.nithingodugu.ecommerce.authservice.repository;

import com.nithingodugu.ecommerce.authservice.domain.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenIdAndRevokedFalse(String tokenId);
}
