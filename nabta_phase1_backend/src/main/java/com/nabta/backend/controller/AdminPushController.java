package com.nabta.backend.controller;

import com.nabta.backend.dto.PushDtos.PushStatusResponse;
import com.nabta.backend.service.PushNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/push")
public class AdminPushController {
    private final PushNotificationService push;
    public AdminPushController(PushNotificationService push) { this.push = push; }

    @GetMapping("/status")
    public PushStatusResponse status() { return push.status(); }
}
