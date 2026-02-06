package com.nithingodugu.ecommerce.authservice.repository;

import com.nithingodugu.ecommerce.authservice.domain.entity.RefreshToken;
import com.nithingodugu.ecommerce.authservice.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenIdAndRevokedFalse(String tokenId);
}
