package com.nabta.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class PushDtos {
    private PushDtos() {}

    public record RegisterDeviceTokenRequest(
        @NotBlank @Size(max = 500) String token,
        @NotBlank @Size(max = 20) String platform
    ) {}

    public record PushStatusResponse(boolean configured, long registeredAdminDevices) {}
}
