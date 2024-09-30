package com.sellersphere.authorization;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public final class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JWTVerifier verifier;

    public JwtAuthorizationFilter(@Value("${security.jwt.issuer:https://seller-sphere.com}") String issuer,
                                  @Value("${security.jwt.secret}") String secret) {
        this.verifier = JWT.require(Algorithm.HMAC256(secret))
                .withIssuer(issuer)
                .withClaimPresence("Scope")
                .withClaimPresence("sub")
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var authorization = request.getHeader("Authorization");
        if (authorization == null || !(authorization.startsWith("Bearer "))){
            filterChain.doFilter(request, response);
            return;
        }
        DecodedJWT jwt;
        try {
            jwt = verifier.verify(authorization.substring("Bearer ".length()));
        } catch (JWTVerificationException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (!jwt.getClaim("Scope").asString().equals("user")) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        var userId = jwt.getSubject();
        try {
            AuthorizedUser.AUTHORIZED_USER_THREAD_LOCAL.set(new AuthorizedUser(userId));
            filterChain.doFilter(request, response);
        }
        finally {
            AuthorizedUser.AUTHORIZED_USER_THREAD_LOCAL.remove();
        }
    }
}
