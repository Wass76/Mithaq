package com.Shakwa.notification.service;

import com.Shakwa.complaint.entity.Complaint;
import com.Shakwa.complaint.Enum.ComplaintStatus;
import com.Shakwa.notification.dto.NotificationRequest;
import com.Shakwa.user.entity.Citizen;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration service to send notifications related to complaints
 * This demonstrates how to integrate notifications with existing features
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintNotificationIntegration {

    private final NotificationService notificationService;

    /**
     * Send notification when complaint status changes
     */
    public void notifyComplaintStatusChange(Complaint complaint, ComplaintStatus oldStatus, ComplaintStatus newStatus) {
        if (complaint.getCitizen() == null) {
            log.warn("Complaint {} has no citizen, skipping notification", complaint.getId());
            return;
        }

        Citizen citizen = complaint.getCitizen();
        String title = "تحديث حالة الشكوى";
        String body = buildStatusChangeMessage(complaint, oldStatus, newStatus);

        Map<String, String> data = new HashMap<>();
        data.put("complaintId", String.valueOf(complaint.getId()));
        data.put("trackingNumber", complaint.getTrackingNumber());
        data.put("oldStatus", oldStatus != null ? oldStatus.name() : "");
        data.put("newStatus", newStatus.name());
        data.put("type", "complaint_status_change");

        NotificationRequest request = NotificationRequest.builder()
                .userId(citizen.getId())
                .title(title)
                .body(body)
                .data(data)
                .notificationType("complaint_update")
                .clickAction("/complaints/" + complaint.getId())
                .build();

        try {
            notificationService.sendNotification(request);
            log.info("Status change notification sent for complaint {}", complaint.getId());
        } catch (Exception e) {
            log.error("Failed to send status change notification for complaint {}: {}", 
                    complaint.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send notification when complaint receives a response
     */
    public void notifyComplaintResponse(Complaint complaint, String response) {
        if (complaint.getCitizen() == null) {
            log.warn("Complaint {} has no citizen, skipping notification", complaint.getId());
            return;
        }

        Citizen citizen = complaint.getCitizen();
        String title = "رد جديد على الشكوى";
        String body = String.format("تم الرد على شكواك رقم %s: %s", 
                complaint.getTrackingNumber(), 
                response.length() > 100 ? response.substring(0, 100) + "..." : response);

        Map<String, String> data = new HashMap<>();
        data.put("complaintId", String.valueOf(complaint.getId()));
        data.put("trackingNumber", complaint.getTrackingNumber());
        data.put("type", "complaint_response");

        NotificationRequest request = NotificationRequest.builder()
                .userId(citizen.getId())
                .title(title)
                .body(body)
                .data(data)
                .notificationType("complaint_response")
                .clickAction("/complaints/" + complaint.getId())
                .build();

        try {
            notificationService.sendNotification(request);
            log.info("Response notification sent for complaint {}", complaint.getId());
        } catch (Exception e) {
            log.error("Failed to send response notification for complaint {}: {}", 
                    complaint.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send notification when a new complaint is created
     */
    public void notifyComplaintCreated(Complaint complaint) {
        if (complaint.getCitizen() == null) {
            log.warn("Complaint {} has no citizen, skipping notification", complaint.getId());
            return;
        }

        Citizen citizen = complaint.getCitizen();
        String title = "تم إنشاء الشكوى بنجاح";
        String body = String.format("تم إنشاء شكواك برقم تتبع: %s", complaint.getTrackingNumber());

        Map<String, String> data = new HashMap<>();
        data.put("complaintId", String.valueOf(complaint.getId()));
        data.put("trackingNumber", complaint.getTrackingNumber());
        data.put("type", "complaint_created");

        NotificationRequest request = NotificationRequest.builder()
                .userId(citizen.getId())
                .title(title)
                .body(body)
                .data(data)
                .notificationType("complaint_created")
                .clickAction("/complaints/" + complaint.getId())
                .build();

        try {
            notificationService.sendNotification(request);
            log.info("Creation notification sent for complaint {}", complaint.getId());
        } catch (Exception e) {
            log.error("Failed to send creation notification for complaint {}: {}", 
                    complaint.getId(), e.getMessage(), e);
        }
    }

    /**
     * Send notification when employee requests additional information
     */
    public void notifyInfoRequested(Complaint complaint, com.Shakwa.complaint.entity.InformationRequest request) {
        if (complaint.getCitizen() == null) {
            log.warn("Complaint {} has no citizen, skipping notification", complaint.getId());
            return;
        }

        Citizen citizen = complaint.getCitizen();
        String title = "طلب معلومات إضافية";
        String body = String.format("تم طلب معلومات إضافية بخصوص شكواك رقم %s: %s", 
                complaint.getTrackingNumber(),
                request.getRequestMessage().length() > 100 
                    ? request.getRequestMessage().substring(0, 100) + "..." 
                    : request.getRequestMessage());

        Map<String, String> data = new HashMap<>();
        data.put("complaintId", String.valueOf(complaint.getId()));
        data.put("trackingNumber", complaint.getTrackingNumber());
        data.put("requestId", String.valueOf(request.getId()));
        data.put("type", "info_requested");

        NotificationRequest notificationRequest = NotificationRequest.builder()
                .userId(citizen.getId())
                .title(title)
                .body(body)
                .data(data)
                .notificationType("info_requested")
                .clickAction("/complaints/" + complaint.getId())
                .build();

        try {
            notificationService.sendNotification(notificationRequest);
            log.info("Info request notification sent for complaint {}", complaint.getId());
        } catch (Exception e) {
            log.error("Failed to send info request notification for complaint {}: {}", 
                    complaint.getId(), e.getMessage(), e);
        }
    }

    private String buildStatusChangeMessage(Complaint complaint, ComplaintStatus oldStatus, ComplaintStatus newStatus) {
        String trackingNumber = complaint.getTrackingNumber();
        
        switch (newStatus) {
            case IN_PROGRESS:
                return String.format("تم بدء معالجة شكواك رقم %s", trackingNumber);
            case RESOLVED:
                return String.format("تم حل شكواك رقم %s", trackingNumber);
            case REJECTED:
                return String.format("تم رفض شكواك رقم %s", trackingNumber);
            case CLOSED:
                return String.format("تم إغلاق شكواك رقم %s", trackingNumber);
            default:
                return String.format("تم تحديث حالة شكواك رقم %s إلى %s", trackingNumber, newStatus);
        }
    }
}
