package com.crazym8nd.notes.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JwtRealmRolesConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS_CLAIM = "realm_access";
    private static final String ROLES_CLAIM = "roles";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        List<SimpleGrantedAuthority> authorities = extractRoles(jwt).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new JwtAuthenticationToken(jwt, authorities);
    }

    private static List<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
        if (realmAccess instanceof Map<?, ?> realmAccessMap
                && realmAccessMap.get(ROLES_CLAIM) instanceof Collection<?> roles) {
            return roles.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }
}
