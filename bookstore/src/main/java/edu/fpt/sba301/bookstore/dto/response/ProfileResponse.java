package edu.fpt.sba301.bookstore.dto.response;

public record ProfileResponse(
    Long id,
    String email,
    String fullName,
    String role,
    Long points,
    String tier,
    Long lifetimePoints,
    String avatarUrl
) {}
