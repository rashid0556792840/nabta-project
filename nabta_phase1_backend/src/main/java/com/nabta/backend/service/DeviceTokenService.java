package com.nabta.backend.service;

import com.nabta.backend.dto.PushDtos.RegisterDeviceTokenRequest;
import com.nabta.backend.exception.ApiException;
import com.nabta.backend.model.AppUser;
import com.nabta.backend.model.DeviceToken;
import com.nabta.backend.model.Role;
import com.nabta.backend.repository.DeviceTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class DeviceTokenService {
    private final DeviceTokenRepository devices;
    public DeviceTokenService(DeviceTokenRepository devices) { this.devices = devices; }

    @Transactional
    public void register(AppUser current, RegisterDeviceTokenRequest request) {
        requireOperations(current);
        String token = request.token().trim();
        String platform = request.platform().trim().toUpperCase(Locale.ROOT);
        if (!platform.equals("ANDROID")) throw ApiException.badRequest("Only Android device tokens are supported in this release.");
        DeviceToken device = devices.findByFcmToken(token).orElseGet(DeviceToken::new);
        device.setUser(current);
        device.setFcmToken(token);
        device.setPlatform(platform);
        devices.save(device);
    }

    @Transactional
    public void unregister(AppUser current, String rawToken) {
        requireOperations(current);
        if (rawToken == null || rawToken.isBlank()) throw ApiException.badRequest("A device token is required.");
        devices.deleteByUserIdAndFcmToken(current.getId(), rawToken.trim());
    }

    private void requireOperations(AppUser user) {
        if (user == null || (user.getRole() != Role.ADMIN && user.getRole() != Role.SUPERVISOR && user.getRole() != Role.DELIVERY)) throw ApiException.forbidden("Only Owner, Supervisor, or Delivery accounts can register or remove operational notification devices.");
    }
}
