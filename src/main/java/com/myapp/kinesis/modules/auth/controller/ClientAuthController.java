package com.myapp.kinesis.modules.auth.controller;

import com.myapp.kinesis.common.dto.ApiResponse;
import com.myapp.kinesis.modules.auth.dto.ClientLoginRequest;
import com.myapp.kinesis.modules.auth.dto.ClientRegistrationRequest;
import com.myapp.kinesis.modules.auth.dto.LoginResponse;
import com.myapp.kinesis.modules.auth.service.ClientAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController for the LOW-SECURITY "Air-Gap".
 * Handles all public, un-authenticated requests for the
 * Customer-facing portals (e.g., from clinic-a.kinesis.io).
 */
@RestController
@RequestMapping("/api/auth/customer") // Note the '/customer' path prefix
public class ClientAuthController {

    private final ClientAuthService clientAuthService;

    public ClientAuthController(ClientAuthService clientAuthService) {
        this.clientAuthService = clientAuthService;
    }

    /**
     * Endpoint for a new Client (patient/member) to sign up
     * *at a specific vendor*.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
        clientAuthService.registerClient(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Customer registered successfully!"));
    }

    /**
     * Endpoint for a Client to log in *to a specific vendor's portal*.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody ClientLoginRequest request) {
        LoginResponse response = clientAuthService.login(request);
        return ResponseEntity.ok(response);
    }
}