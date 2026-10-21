package com.nabta.backend.controller;

import com.nabta.backend.dto.PushDtos.RegisterDeviceTokenRequest;
import com.nabta.backend.dto.PushDtos.PushStatusResponse;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.service.DeviceTokenService;
import com.nabta.backend.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/device-tokens")
public class DeviceTokenController {
    private final DeviceTokenService devices;
    public DeviceTokenController(DeviceTokenService devices) { this.devices = devices; }

    @PostMapping
    public ResponseEntity<Void> register(@AuthenticationPrincipal AppUser current, @Valid @RequestBody RegisterDeviceTokenRequest request) {
        devices.register(current, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unregister(@AuthenticationPrincipal AppUser current, @RequestParam String token) {
        devices.unregister(current, token);
        return ResponseEntity.noContent().build();
    }
}
