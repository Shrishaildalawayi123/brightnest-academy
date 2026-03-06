package com.shrishailacademy.tenant;

/**
 * Request-scoped tenant context.
 *
 * For now we store the numeric tenantId (FK) and the external tenantKey.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_KEY = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, String tenantKey) {
        TENANT_ID.set(tenantId);
        TENANT_KEY.set(tenantKey);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static String getTenantKey() {
        return TENANT_KEY.get();
    }

    public static Long requireTenantId() {
        Long id = TENANT_ID.get();
        if (id == null) {
            throw new IllegalStateException("Tenant context is missing");
        }
        return id;
    }

    public static void clear() {
        TENANT_ID.remove();
        TENANT_KEY.remove();
    }
}
