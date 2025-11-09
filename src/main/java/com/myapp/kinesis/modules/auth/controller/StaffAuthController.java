package com.myapp.kinesis.modules.auth.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.auth.dto.LoginResponse;
import com.myapp.kinesis.modules.auth.dto.StaffLoginRequest;
import com.myapp.kinesis.modules.auth.dto.VendorRegistrationRequest;
import com.myapp.kinesis.modules.auth.service.StaffAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController for the HIGH-SECURITY "Air-Gap".
 * Handles all public, un-authenticated requests for the
 * Staff/Vendor/Admin side of the platform (e.g., from admin.kinesis.io).
 */
@RestController
@RequestMapping("/api/auth/admin") // Note the '/admin' path prefix
public class StaffAuthController {

    private final StaffAuthService staffAuthService;

    public StaffAuthController(StaffAuthService staffAuthService) {
        this.staffAuthService = staffAuthService;
    }

    /**
     * Endpoint for a new Vendor to sign up on the main kinesis.io website.
     * This creates the Staff, the Vendor, and the StaffRole.
     */
    @PostMapping("/register-vendor")
    public ResponseEntity<ApiResponse<?>> registerVendor(@Valid @RequestBody VendorRegistrationRequest request) {
        // The try-catch is handled by our GlobalExceptionHandler
        staffAuthService.registerVendor(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vendor registered successfully!"));
    }

    /**
     * Endpoint for any Staff member (Owner or Staff) to log in.
     * The request MUST specify which vendor "silo" (slug) they are logging into.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody StaffLoginRequest request) {
        LoginResponse response = staffAuthService.login(request);
        return ResponseEntity.ok(response);
    }
}