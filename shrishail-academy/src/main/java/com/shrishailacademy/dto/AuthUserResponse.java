package com.shrishailacademy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Safe auth response for cookie-based authentication.
 *
 * Does not expose access tokens in the response body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
}
