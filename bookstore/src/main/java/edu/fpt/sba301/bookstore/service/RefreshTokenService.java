package edu.fpt.sba301.bookstore.service;

import edu.fpt.sba301.bookstore.entity.RefreshToken;
import edu.fpt.sba301.bookstore.entity.User;

public interface RefreshTokenService {
    RefreshToken createTokenForUser(User user);
    RefreshToken validateAndGet(String token); // throws if invalid
    RefreshToken rotate(RefreshToken oldToken); // create new, revoke old
    void revoke(RefreshToken token);
    void cleanupExpiredTokens();
}