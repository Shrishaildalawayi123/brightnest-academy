package com.shrishailacademy.integration.support;

import com.shrishailacademy.model.Tenant;
import com.shrishailacademy.model.User;
import jakarta.servlet.http.Cookie;

public record TenantAdminSession(Tenant tenant, User admin, Cookie authCookie, Cookie csrfCookie) {
}
