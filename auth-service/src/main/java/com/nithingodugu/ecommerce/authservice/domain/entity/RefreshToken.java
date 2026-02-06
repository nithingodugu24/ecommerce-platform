package com.nithingodugu.ecommerce.authservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens",
        indexes = @Index(columnList = "tokenId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @Column(nullable = false, updatable = false)
    private String tokenId;

    @Column(nullable = false)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Instant expiry;

    private boolean revoked;

    public RefreshToken(String tokenId, String tokenHash, User user, Instant expiry){
        this.tokenId = tokenId;
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiry = expiry;
        this.revoked = false;
    }

    public void revoke(){
        this.revoked = true;
    }

}
