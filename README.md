# Kinesis
 ## Improvements...........

## CRITICAL BLOCKERS (Must Fix Before Launch)
1. ### Security Vulnerabilities

``` java
// ❌ CRITICAL: Secrets Exposed in application-dev.properties
spring.datasource.password=Ramayan786@
app.jwt.secret=ghgdgdgfshdgfftrgfxfc21324t56rry(*&^%jhfhgf
razorpay.key_secret=

// ✅ FIX: Use environment variables ONLY
@Value("${JWT_SECRET}")  // Never default values for production
@Value("${DB_PASSWORD}")
@Value("${RAZORPAY_SECRET}")
```
### Action Items:

1. Immediate: Remove ALL secrets from properties files
2. Required: Implement AWS Secrets Manager / HashiCorp Vault
3. Add: Secret rotation policy (90-day rotation)

``` # Environment-based configuration
export JWT_SECRET=$(openssl rand -base64 32)
export DB_PASSWORD=$(aws secretsmanager get-secret-value --secret-id prod/db/password)
```

2. ### Double Booking Race Condition
  ``` java
// CURRENT: Time-of-check to time-of-use vulnerability
private boolean isSlotAvailable(...) {
    List<AppointmentEntity> conflicts = appointmentRepository.findConflictingAppointments(...);
    if (!conflicts.isEmpty()) return false;  // ⚠️ Another request can book here!
    // ... booking logic
}
// ✅ FIX: Database-level locking
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT a FROM AppointmentEntity a 
    WHERE a.vendor.id = :vendorId 
    AND ar.resource.id IN :resourceIds
    AND (a.startTime < :endTime AND a.endTime > :startTime)
    FOR UPDATE NOWAIT
""")
```
### Alternative: Add Unique Constraint
``` java 
-- PostgreSQL constraint
CREATE UNIQUE INDEX idx_no_double_booking ON appointments (
    service_id, start_time, 
    (CASE WHEN status NOT IN ('CANCELLED', 'NO_SHOW') THEN 1 ELSE NULL END)
);
```

3. ### HIGH PRIORITY: No Rate Limiting
   ``` java
   // ✅ ADD: Bucket4j rate limiting
   @Configuration
   public class RateLimitConfig {
    
    @Bean
    public RateLimiter apiRateLimiter() {
        Bandwidth limit = Bandwidth.builder()
            .capacity(100)              // 100 requests
            .refillGreedy(100, Duration.ofMinutes(1))
            .build();
            
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }
   }
   @Component
   public class RateLimitInterceptor extends HandlerInterceptorAdapter {
    
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String clientId = request.getRemoteAddr();
        
        if (!rateLimiter.tryConsume(1)) {
            response.setStatus(429);  // Too Many Requests
            response.getWriter().write("Rate limit exceeded");
            return false;
        }
        
        return true;
    }
   }
   ```
4. ### N+1 Query Problem
``` java
// ❌ CURRENT: ServiceService.getServicesForCurrentVendor()
return serviceRepository.findAllByVendorId(vendorId).stream()
    .map(service -> {
        List<ServiceResourceEntity> links = serviceResourceRepository
            .findAllByServiceId(service.getId());  // N queries!
        return ServiceResponseDto.fromEntity(service, links);
    })
    .collect(Collectors.toList());

// ✅ FIX: Use JOIN FETCH
@Query("""
    SELECT DISTINCT s FROM ServiceEntity s
    LEFT JOIN FETCH s.serviceResources sr
    LEFT JOIN FETCH sr.resource
    WHERE s.vendor.id = :vendorId
""")
List<ServiceEntity> findAllByVendorIdWithResources(@Param("vendorId") UUID vendorId);
```

### Add to ServiceEntity:
``` java
@OneToMany(mappedBy = "service", fetch = FetchType.LAZY)
private List<ServiceResourceEntity> serviceResources = new ArrayList<>();
```
