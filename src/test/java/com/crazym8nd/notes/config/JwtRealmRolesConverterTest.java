package com.crazym8nd.notes.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtRealmRolesConverterTest {

    private final JwtRealmRolesConverter converter = new JwtRealmRolesConverter();

    @Test
    @DisplayName("realm_access.roles превращаются в authorities")
    void shouldMapRealmRolesToAuthorities() {
        Jwt jwt = jwtWithClaim(Map.of("realm_access", Map.of("roles", List.of("notes-reader", "notes-writer"))));

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("notes-reader", "notes-writer");
    }

    @Test
    @DisplayName("токен без claim ролей даёт пустой набор authorities")
    void shouldReturnEmptyAuthoritiesWithoutClaim() {
        Jwt jwt = jwtWithClaim(Map.of());

        var authentication = converter.convert(jwt);

        assertThat(authentication.getAuthorities()).isEmpty();
    }

    private static Jwt jwtWithClaim(Map<String, Object> claims) {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("demo")
                .claims(target -> target.putAll(claims))
                .build();
    }
}
