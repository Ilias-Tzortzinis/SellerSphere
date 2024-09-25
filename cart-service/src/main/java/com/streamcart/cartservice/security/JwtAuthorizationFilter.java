package com.streamcart.cartservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public final class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JWTVerifier verifier;

    public JwtAuthorizationFilter(@Value("${security.jwt.secret-key}") String secretKey) {
        verifier = JWT.require(Algorithm.HMAC256(secretKey))
                .withClaim("scope", "user")
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        try {
            var jwt = verifier.verify(authorization.substring("Bearer ".length()));
            var userId = jwt.getSubject();
            AuthorizedUser.AUTHORIZED_USER_THREAD_LOCAL.set(new AuthorizedUser(userId));
            try {
                filterChain.doFilter(request, response);
            }
            finally {
                AuthorizedUser.AUTHORIZED_USER_THREAD_LOCAL.remove();
            }
        }
        catch (JWTVerificationException e){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
