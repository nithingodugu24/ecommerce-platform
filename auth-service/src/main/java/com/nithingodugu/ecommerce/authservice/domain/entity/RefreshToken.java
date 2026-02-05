package com.nithingodugu.ecommerce.authservice.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Instant expiry;

    private boolean revoked;

    public RefreshToken(String token, User user, Instant expiry){
        this.token = token;
        this.user = user;
        this.expiry = expiry;
        this.revoked = false;
    }

    public void revoke(){
        this.revoked = true;
    }

}
