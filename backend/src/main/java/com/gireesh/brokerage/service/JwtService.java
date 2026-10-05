package com.gireesh.brokerage.service;

import com.gireesh.brokerage.config.JwtSettings;
import com.gireesh.brokerage.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class JwtService {
    private final JwtEncoder jwtEncoder;
    private final JwtSettings settings;
    private final Clock clock;

    @Autowired
    public JwtService(JwtEncoder jwtEncoder, JwtSettings settings) {
        this(jwtEncoder, settings, Clock.systemUTC());
    }

    JwtService(JwtEncoder jwtEncoder, JwtSettings settings, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.settings = settings;
        this.clock = clock;
    }

    public String issue(User user) {
        Instant issuedAt = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(settings.getExpiry()))
                .claim("role", user.getRole().name())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
