package edu.fpt.sba301.bookstore.service.imp;

import edu.fpt.sba301.bookstore.config.properties.JwtProperties;
import edu.fpt.sba301.bookstore.entity.RefreshToken;
import edu.fpt.sba301.bookstore.entity.User;
import edu.fpt.sba301.bookstore.repository.RefreshTokenRepository;
import edu.fpt.sba301.bookstore.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    public RefreshToken createTokenForUser(User user) {
        RefreshToken token = new RefreshToken();
        // generate a secure random token (UUID here). Consider using more entropy if needed.
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        token.setToken(rawToken);

        token.setUser(user);
        OffsetDateTime now = OffsetDateTime.now();
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusNanos(jwtProperties.refreshExpiration() * 1_000_000L)); // ms -> nanos
        token.setRevoked(false);

        return refreshTokenRepository.save(token);
    }

    @Override
    public RefreshToken validateAndGet(String tokenStr) {
        RefreshToken token = refreshTokenRepository.findByToken(tokenStr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (token.getRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revoked");
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        if (Boolean.FALSE.equals(token.getUser().getEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User disabled");
        }
        return token;
    }

    @Override
    public RefreshToken rotate(RefreshToken oldToken) {
        // mark old token revoked and create a new one for same user
        oldToken.setRevoked(true);
        // create new token
        RefreshToken newToken = createTokenForUser(oldToken.getUser());
        // store reference
        oldToken.setReplacedByToken(newToken.getToken());
        refreshTokenRepository.save(oldToken); // update old
        // new token already saved by createTokenForUser
        return newToken;
    }

    @Override
    public void revoke(RefreshToken token) {
        token.setRevoked(true);
        refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    @Override
    @Scheduled(cron = "0 0 0 * * ?") // run daily at midnight
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteAllByExpiresAtBefore(OffsetDateTime.now());
    }
}