# Firebase Notification System Implementation - نظام الإشعارات باستخدام Firebase

## 📋 جدول المحتويات

1. [نظرة عامة](#نظرة-عامة)
2. [المكونات الرئيسية](#المكونات-الرئيسية)
3. [الإعداد والتكوين](#الإعداد-والتكوين)
4. [كيفية الاستخدام](#كيفية-الاستخدام)
5. [API Endpoints](#api-endpoints)
6. [التكامل مع النظام](#التكامل-مع-النظام)
7. [Database Schema](#database-schema)
8. [الاختبار](#الاختبار)
9. [Troubleshooting](#troubleshooting)

---

## 🎯 نظرة عامة

تم تطبيق نظام إشعارات شامل باستخدام **Firebase Cloud Messaging (FCM)** لإرسال إشعارات Push للمستخدمين عبر تطبيقات الويب والهواتف المحمولة.

### المميزات الرئيسية:
- ✅ إرسال إشعارات Push للمستخدمين
- ✅ دعم عدة أجهزة للمستخدم الواحد
- ✅ تتبع حالة الإشعارات (مرسل، تم التسليم، فشل)
- ✅ إدارة Tokens للمستخدمين
- ✅ واجهة REST API كاملة
- ✅ التكامل مع نظام الشكاوى الموجود

---

## 📦 المكونات الرئيسية

### 1. Configuration Layer

#### `FirebaseConfig.java`
- **الموقع:** `com.Shakwa.notification.config`
- **الوظيفة:** تهيئة Firebase Admin SDK
- **الميزات:**
  - قراءة Firebase Service Account JSON
  - دعم Environment Variables
  - إنشاء FirebaseMessaging Bean

### 2. Entity Layer

#### `NotificationToken.java`
- **الموقع:** `com.Shakwa.notification.entity`
- **الوظيفة:** تخزين FCM Tokens للمستخدمين
- **الحقول:**
  - `user`: المستخدم المالك للـ Token
  - `token`: FCM Token
  - `deviceType`: نوع الجهاز (web, android, ios)
  - `deviceInfo`: معلومات إضافية عن الجهاز
  - `isActive`: حالة Token (نشط/غير نشط)

#### `Notification.java`
- **الموقع:** `com.Shakwa.notification.entity`
- **الوظيفة:** تخزين سجل الإشعارات المرسلة
- **الحقول:**
  - `user`: المستخدم المستقبل
  - `title`: عنوان الإشعار
  - `body`: محتوى الإشعار
  - `data`: بيانات إضافية (JSON)
  - `status`: حالة الإشعار (PENDING, SENT, DELIVERED, FAILED, READ)
  - `sentAt`: وقت الإرسال
  - `readAt`: وقت القراءة

### 3. Repository Layer

#### `NotificationTokenRepository.java`
- **الوظيفة:** إدارة FCM Tokens
- **الطرق الرئيسية:**
  - `findActiveTokensByUserId()`: الحصول على Tokens نشطة للمستخدم
  - `findByUserAndToken()`: البحث عن Token محدد
  - `deactivateAllTokensByUser()`: تعطيل جميع Tokens للمستخدم

#### `NotificationRepository.java`
- **الوظيفة:** إدارة الإشعارات
- **الطرق الرئيسية:**
  - `findByUserOrderByCreatedAtDesc()`: الحصول على إشعارات المستخدم
  - `findUnreadNotificationsByUser()`: الحصول على الإشعارات غير المقروءة
  - `countUnreadNotificationsByUser()`: عدد الإشعارات غير المقروءة
  - `markAsRead()`: تحديد الإشعار كمقروء

### 4. Service Layer

#### `FirebaseNotificationService.java`
- **الوظيفة:** إرسال الإشعارات عبر Firebase
- **الطرق الرئيسية:**
  - `sendNotification()`: إرسال إشعار لمستخدم واحد
  - `sendNotificationToMultipleUsers()`: إرسال إشعار لعدة مستخدمين
  - `registerToken()`: تسجيل FCM Token
  - `unregisterToken()`: إلغاء تسجيل Token

#### `NotificationService.java`
- **الوظيفة:** إدارة الإشعارات على مستوى الأعمال
- **الطرق الرئيسية:**
  - `getUserNotifications()`: الحصول على إشعارات المستخدم
  - `getUnreadNotifications()`: الحصول على الإشعارات غير المقروءة
  - `markAsRead()`: تحديد الإشعار كمقروء
  - `markAllAsRead()`: تحديد جميع الإشعارات كمقروءة

#### `ComplaintNotificationIntegration.java`
- **الوظيفة:** التكامل مع نظام الشكاوى
- **الطرق الرئيسية:**
  - `notifyComplaintStatusChange()`: إشعار عند تغيير حالة الشكوى
  - `notifyComplaintResponse()`: إشعار عند الرد على الشكوى
  - `notifyComplaintCreated()`: إشعار عند إنشاء شكوى جديدة

### 5. Controller Layer

#### `NotificationController.java`
- **الموقع:** `com.Shakwa.notification.controller`
- **الوظيفة:** REST API لإدارة الإشعارات
- **Endpoints:** راجع قسم [API Endpoints](#api-endpoints)

---

## ⚙️ الإعداد والتكوين

### الخطوة 1: إعداد Firebase Project

1. اذهب إلى [Firebase Console](https://console.firebase.google.com/)
2. أنشئ مشروع جديد أو استخدم مشروع موجود
3. أضف تطبيق Web:
   - Settings > Project Settings > General
   - Add App > Web
   - سجل اسم التطبيق

### الخطوة 2: الحصول على Service Account Key

1. في Firebase Console:
   - Settings > Project Settings > Service Accounts
   - انقر على "Generate New Private Key"
   - احفظ الملف JSON

2. ضع الملف في المشروع:
   ```
   src/main/resources/firebase-service-account.json
   ```

### الخطوة 3: تكوين application.properties

تم إضافة الإعدادات التالية:

```properties
# Firebase Configuration
firebase.service-account.path=firebase-service-account.json
firebase.service-account.env=FIREBASE_SERVICE_ACCOUNT
```

**ملاحظة:** يمكنك أيضاً استخدام Environment Variable:
```bash
export FIREBASE_SERVICE_ACCOUNT=/path/to/firebase-service-account.json
```

### الخطوة 4: تشغيل Database Migration

تم إنشاء ملف Migration:
```
src/main/resources/db/migration/V4__create_notification_tables.sql
```

لتفعيل Flyway:
```properties
spring.flyway.enabled=true
```

أو قم بتشغيل SQL يدوياً.

---

## 🚀 كيفية الاستخدام

### 1. تسجيل FCM Token من Frontend

```javascript
// في Frontend (React/Vue/Angular)
import { getMessaging, getToken } from "firebase/messaging";

const messaging = getMessaging();
const token = await getToken(messaging, {
  vapidKey: "YOUR_VAPID_KEY" // من Firebase Console > Cloud Messaging
});

// إرسال Token للـ Backend
await fetch('/api/v1/notifications/register-token', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${userToken}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    token: token,
    deviceType: 'web',
    deviceInfo: navigator.userAgent
  })
});
```

### 2. إرسال إشعار من Backend

```java
@Autowired
private NotificationService notificationService;

// إرسال إشعار لمستخدم
NotificationRequest request = NotificationRequest.builder()
    .userId(1L)
    .title("إشعار جديد")
    .body("لديك إشعار جديد")
    .notificationType("system_alert")
    .data(Map.of("key", "value"))
    .build();

NotificationResponse response = notificationService.sendNotification(request);
```

### 3. التكامل مع نظام الشكاوى

```java
@Autowired
private ComplaintNotificationIntegration complaintNotificationIntegration;

// عند تغيير حالة الشكوى
complaintNotificationIntegration.notifyComplaintStatusChange(
    complaint, 
    oldStatus, 
    newStatus
);

// عند الرد على الشكوى
complaintNotificationIntegration.notifyComplaintResponse(
    complaint, 
    "تم حل المشكلة"
);
```

---

## 🌐 API Endpoints

### 1. تسجيل FCM Token

```http
POST /api/v1/notifications/register-token
Authorization: Bearer <token>
Content-Type: application/json

{
  "token": "fcm_token_here",
  "deviceType": "web",
  "deviceInfo": "Mozilla/5.0..."
}
```

**Response:**
```json
{
  "data": {
    "id": 1,
    "token": "fcm_token_here",
    "deviceType": "web",
    "isActive": true
  },
  "message": "Token registered successfully"
}
```

### 2. إلغاء تسجيل Token

```http
DELETE /api/v1/notifications/unregister-token/{token}
Authorization: Bearer <token>
```

### 3. الحصول على الإشعارات

```http
GET /api/v1/notifications?page=0&size=20&sortBy=createdAt&sortDir=DESC
Authorization: Bearer <token>
```

**Response:**
```json
{
  "body": [
    {
      "id": 1,
      "title": "إشعار جديد",
      "body": "لديك إشعار جديد",
      "status": "SENT",
      "readAt": null,
      "createdAt": "2024-01-15T10:30:00"
    }
  ],
  "pageable": {
    "page": 0,
    "perPage": 20,
    "total": 50
  }
}
```

### 4. الحصول على الإشعارات غير المقروءة

```http
GET /api/v1/notifications/unread
Authorization: Bearer <token>
```

### 5. عدد الإشعارات غير المقروءة

```http
GET /api/v1/notifications/unread/count
Authorization: Bearer <token>
```

**Response:**
```json
{
  "data": {
    "count": 5
  }
}
```

### 6. تحديد إشعار كمقروء

```http
PUT /api/v1/notifications/{id}/read
Authorization: Bearer <token>
```

### 7. تحديد جميع الإشعارات كمقروءة

```http
PUT /api/v1/notifications/read-all
Authorization: Bearer <token>
```

### 8. إرسال إشعار (Admin)

```http
POST /api/v1/notifications/send
Authorization: Bearer <admin_token>
Content-Type: application/json

{
  "userId": 1,
  "title": "إشعار مهم",
  "body": "هذا إشعار مهم",
  "notificationType": "system_alert",
  "data": {
    "key": "value"
  },
  "clickAction": "/complaints/123"
}
```

---

## 🔗 التكامل مع النظام

### مثال: إرسال إشعار عند تغيير حالة الشكوى

في `ComplaintService.java`:

```java
@Autowired
private ComplaintNotificationIntegration notificationIntegration;

public ComplaintDTOResponse respondToComplaint(Long id, String response, ComplaintStatus status) {
    Complaint complaint = findById(id);
    ComplaintStatus oldStatus = complaint.getStatus();
    
    // تحديث الشكوى
    complaint.setStatus(status);
    complaint.setResponse(response);
    complaint = complaintRepository.save(complaint);
    
    // إرسال إشعار
    notificationIntegration.notifyComplaintStatusChange(complaint, oldStatus, status);
    
    return complaintMapper.toDTO(complaint);
}
```

---

## 🗄️ Database Schema

### notification_tokens Table

```sql
CREATE TABLE notification_tokens (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(1000) NOT NULL,
    device_type VARCHAR(50),
    device_info VARCHAR(500),
    last_used_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    last_modified_by BIGINT,
    UNIQUE (user_id, token),
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

### notifications Table

```sql
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    data TEXT,
    status VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP,
    read_at TIMESTAMP,
    error_message VARCHAR(1000),
    notification_type VARCHAR(100),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    created_by BIGINT NOT NULL,
    last_modified_by BIGINT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 🧪 الاختبار

### اختبار 1: تسجيل Token

```bash
curl -X POST http://localhost:13000/api/v1/notifications/register-token \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "test_token_123",
    "deviceType": "web"
  }'
```

### اختبار 2: إرسال إشعار

```bash
curl -X POST http://localhost:13000/api/v1/notifications/send \
  -H "Authorization: Bearer <admin_token>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "title": "اختبار",
    "body": "هذا إشعار تجريبي"
  }'
```

### اختبار 3: الحصول على الإشعارات

```bash
curl -X GET http://localhost:13000/api/v1/notifications \
  -H "Authorization: Bearer <token>"
```

---

## 🔧 Troubleshooting

### المشكلة: Firebase initialization failed

**الأسباب المحتملة:**
1. ملف Service Account غير موجود
2. مسار الملف غير صحيح
3. صلاحيات الملف غير صحيحة

**الحل:**
- تأكد من وجود `firebase-service-account.json` في `src/main/resources/`
- أو استخدم Environment Variable: `FIREBASE_SERVICE_ACCOUNT`

### المشكلة: Token registration fails

**الأسباب المحتملة:**
1. Token غير صحيح
2. المستخدم غير موجود

**الحل:**
- تأكد من صحة FCM Token
- تأكد من وجود المستخدم في قاعدة البيانات

### المشكلة: Notifications not received

**الأسباب المحتملة:**
1. Token غير نشط
2. المستخدم لم يمنح صلاحيات الإشعارات
3. Firebase configuration غير صحيح

**الحل:**
- تحقق من حالة Token في قاعدة البيانات
- تأكد من منح صلاحيات الإشعارات في المتصفح
- تحقق من Firebase Console logs

### المشكلة: Invalid token error

**الأسباب المحتملة:**
1. Token منتهي الصلاحية
2. Token تم حذفه من Firebase

**الحل:**
- النظام يقوم تلقائياً بتعطيل Tokens غير صحيحة
- المستخدم يحتاج لتسجيل Token جديد

---

## 📝 ملاحظات مهمة

1. **Security:** تأكد من حماية `/api/v1/notifications/send` endpoint (Admin only)
2. **Error Handling:** النظام يتعامل مع الأخطاء تلقائياً ويسجلها
3. **Token Management:** Tokens غير صحيحة يتم تعطيلها تلقائياً
4. **Multiple Devices:** المستخدم يمكنه تسجيل عدة Tokens (عدة أجهزة)
5. **Cleanup:** يمكن إضافة Scheduled Task لحذف Tokens والإشعارات القديمة

---

## ✅ Checklist للاختبار

- [ ] Firebase configuration صحيح
- [ ] Service Account JSON موجود
- [ ] Database migration تم تشغيله
- [ ] Token registration يعمل
- [ ] إرسال إشعارات يعمل
- [ ] استقبال إشعارات في Frontend
- [ ] Mark as read يعمل
- [ ] التكامل مع نظام الشكاوى يعمل

---

## 📚 المراجع

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [Firebase Admin SDK for Java](https://firebase.google.com/docs/admin/setup)
- [FCM HTTP v1 API](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)

---

**آخر تحديث:** 2024-01-15

**المطور:** Auto (Cursor AI)
