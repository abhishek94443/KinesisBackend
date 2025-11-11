package com.myapp.kinesis.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A "backpack" (using ThreadLocal) to securely hold the
 * vendorId for the duration of a single request.
 * <p>
 * This is the core of our Application-Level Tenancy (ALT).
 * It is production-safe and works with Java 17/21 (including Virtual Threads,
 * as long as we clear it properly in the Interceptor).
 */
@Component
public class TenantContext {

    private static final Logger logger = LoggerFactory.getLogger(TenantContext.class);

    // A ThreadLocal variable is "private" to each thread (request).
    // This is the fundamental mechanism that prevents data leaks
    // between concurrent requests in a multi-threaded environment.
    private static final ThreadLocal<UUID> currentVendorId = new ThreadLocal<>();

    /**
     * Sets the vendorId for the current request's thread.
     * This is called by the TenantInterceptor.
     */
    public void setVendorId(UUID vendorId) {
        if (vendorId != null) {
            logger.trace("Setting TenantContext vendorId: {}", vendorId);
            currentVendorId.set(vendorId);
        } else {
            logger.warn("Attempted to set a null vendorId in TenantContext.");
        }
    }

    /**
     * Gets the vendorId for the current request's thread.
     * This is called by our Repositories or Services to build queries.
     *
     * @return The UUID of the current vendor.
     * @throws IllegalStateException if no vendorId is set (a critical security failure).
     */
    public UUID getVendorId() {
        UUID vendorId = currentVendorId.get();
        if (vendorId == null) {
            // If this happens, it means a secure endpoint was called
            // without the TenantInterceptor correctly setting the ID.
            // We MUST fail-fast to prevent data leaks.
            throw new IllegalStateException("TenantContext: No Vendor ID is set for the current request.");
        }
        return vendorId;
    }

    /**
     * Checks if a vendorId is present in the context.
     */
    public boolean isVendorIdSet() {
        return currentVendorId.get() != null;
    }

    /**
     * Clears the vendorId from the thread.
     * This is critical to prevent data leaks between pooled threads.
     * This is called by the TenantInterceptor after the request is finished.
     */
    public void clear() {
        logger.trace("Clearing TenantContext for thread: {}", Thread.currentThread().getName());
        currentVendorId.remove();
    }
}