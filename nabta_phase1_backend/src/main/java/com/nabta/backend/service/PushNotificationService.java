package com.nabta.backend.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.nabta.backend.dto.PushDtos.PushStatusResponse;
import com.nabta.backend.event.OrderCreatedEvent;
import com.nabta.backend.event.OrderWorkflowEvent;
import com.nabta.backend.model.DeviceToken;
import com.nabta.backend.model.Role;
import com.nabta.backend.repository.DeviceTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.ArrayList;
import java.util.List;

@Service
public class PushNotificationService {
    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);
    private final DeviceTokenRepository devices;
    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;

    public PushNotificationService(DeviceTokenRepository devices, ObjectProvider<FirebaseMessaging> firebaseMessaging) {
        this.devices = devices;
        this.firebaseMessaging = firebaseMessaging;
    }

    public PushStatusResponse status() {
        return new PushStatusResponse(firebaseMessaging.getIfAvailable() != null,
            devices.countByUserRole(Role.ADMIN) + devices.countByUserRole(Role.SUPERVISOR) + devices.countByUserRole(Role.DELIVERY));
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyOperationsOfNewOrder(OrderCreatedEvent event) {
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            log.debug("FCM is disabled; order {} was saved without a push notification.", event.orderNumber());
            return;
        }
        List<DeviceToken> recipients = new ArrayList<>();
        recipients.addAll(devices.findAllByUserRole(Role.ADMIN));
        recipients.addAll(devices.findAllByUserRole(Role.SUPERVISOR));
        if (recipients.isEmpty()) {
            log.info("No Owner/Supervisor devices are registered for FCM; order {} was saved successfully.", event.orderNumber());
            return;
        }
        for (DeviceToken device : recipients) {
            Message message = Message.builder()
                .setToken(device.getFcmToken())
                .setNotification(Notification.builder().setTitle("New Nabta order").setBody("Order " + event.orderNumber() + " has been placed.").build())
                .putData("type", "NEW_ORDER")
                .putData("orderId", String.valueOf(event.orderId()))
                .putData("orderNumber", event.orderNumber())
                .build();
            send(messaging, message, "new-order", event.orderNumber());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyWorkflowRecipient(OrderWorkflowEvent event) {
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            log.debug("FCM is disabled; workflow event {} for order {} was not pushed.", event.type(), event.orderNumber());
            return;
        }
        List<DeviceToken> recipients;
        if (event.recipientUserId() != null) recipients = devices.findAllByUserId(event.recipientUserId());
        else if (event.recipientRole() != null) recipients = devices.findAllByUserRole(event.recipientRole());
        else recipients = List.of();
        for (DeviceToken device : recipients) {
            Message.Builder builder = Message.builder().setToken(device.getFcmToken())
                .setNotification(Notification.builder().setTitle(event.title()).setBody(event.body()).build())
                .putData("type", event.type())
                .putData("orderId", String.valueOf(event.orderId()))
                .putData("orderNumber", event.orderNumber());
            if (event.assignmentId() != null) builder.putData("assignmentId", String.valueOf(event.assignmentId()));
            if (event.status() != null) builder.putData("status", event.status());
            send(messaging, builder.build(), event.type(), event.orderNumber());
        }
    }

    private void send(FirebaseMessaging messaging, Message message, String action, String orderNumber) {
        try {
            String messageId = messaging.send(message);
            log.info("FCM accepted notification for action {} / order {} (message id {}).", action, orderNumber, messageId);
        } catch (FirebaseMessagingException ex) {
            // Delivery failures must not roll back a saved order or status change.
            log.warn("Could not send FCM notification for action {} / order {} (error code: {}).", action, orderNumber, ex.getMessagingErrorCode());
        }
    }
}
