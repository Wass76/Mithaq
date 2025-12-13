# خطة عمل: تطبيق متطلبات التقارير والإحصائيات والمشرف العام
## Reports, Statistics & Admin Features Implementation Plan

---

## 📋 ملخص تنفيذي

هذه الخطة تغطي متطلبات:
1. **التقارير للموظف الحكومي** (3 تقارير رئيسية)
2. **لوحة مؤشرات الأداء للمشرف العام** (Dashboard Overview)
3. **إدارة المستخدمين والموظفين**
4. **سجل التدقيق (Audit Log)**
5. **تصدير التقارير (PDF/CSV)**
6. **البحث بالرقم المرجعي**
7. **تعديل كلمة السر**

---

## 🔍 تحليل الوضع الحالي

### ✅ ما هو موجود حالياً:

1. **نظام الأدوار والصلاحيات**:
   - ✅ `PLATFORM_ADMIN` - المشرف العام
   - ✅ `SUPERVISOR` - المشرف الجزئي
   - ✅ `VIEWER` - الموظف العادي
   - ✅ `REPORT_VIEW` permission موجود

2. **نظام Auditing**:
   - ✅ `@Audited` annotation موجود
   - ✅ `AuditAspect` موجود (يسجل في logs حالياً)
   - ✅ `ApplicationAuditingAware` موجود
   - ⚠️ **لكن لا يوجد AuditService أو AuditEvent entity**

3. **إدارة الموظفين**:
   - ✅ `EmployeeService.addEmployee()` - إنشاء موظف
   - ✅ `EmployeeService.updateEmployee()` - تعديل موظف
   - ✅ إدارة الأدوار والصلاحيات موجودة

4. **إدارة الشكاوى**:
   - ✅ جميع عمليات CRUD موجودة
   - ✅ نظام التاريخ (ComplaintHistory) موجود ومكتمل
   - ✅ `ComplaintHistoryService` موجود ويسجل جميع تغييرات الشكاوى
   - ✅ Tracking number موجود

### ❌ ما هو مفقود:

1. **التقارير**:
   - ❌ تقرير إنجاز الشكاوى حسب الحالة
   - ❌ تقرير متوسط زمن الإغلاق
   - ❌ تقرير توزيع الشكاوى حسب النوع

2. **لوحة مؤشرات الأداء**:
   - ❌ Dashboard Overview endpoints
   - ❌ إحصائيات عامة
   - ❌ أكثر الجهات استقبالاً للشكاوى
   - ❌ أكثر أنواع الشكاوى شيوعاً
   - ❌ الشكاوى المتأخرة (Overdue)

3. **سجل التدقيق**:
   - ✅ `ComplaintHistory` موجود لتتبع تغييرات الشكاوى
   - ❌ `AuditEvent` entity (لجميع العمليات في النظام - ليس فقط الشكاوى)
   - ❌ `AuditService` (لجميع العمليات في النظام)
   - ❌ Audit Log viewer API (لجميع العمليات)
   
   **ملاحظة**: `ComplaintHistory` يغطي تغييرات الشكاوى فقط. `AuditEvent` مطلوب لتتبع جميع العمليات الأخرى (إنشاء/تعديل المستخدمين، تسجيل الدخول، إلخ)

4. **تصدير التقارير**:
   - ❌ PDF generation
   - ❌ CSV export

5. **ميزات إضافية**:
   - ❌ البحث بالرقم المرجعي
   - ❌ تعديل كلمة السر

---

## 📐 التصميم المعماري (Architecture Design)

### 1. هيكل الحزم (Package Structure)

```
com.Shakwa/
├── report/
│   ├── service/
│   │   ├── ReportService.java          # Service رئيسي للتقارير
│   │   ├── StatisticsService.java      # Service للإحصائيات
│   │   └── ExportService.java          # Service لتصدير PDF/CSV
│   ├── controller/
│   │   ├── ReportController.java        # Endpoints للتقارير
│   │   └── StatisticsController.java    # Endpoints للإحصائيات
│   ├── dto/
│   │   ├── ComplaintStatusReportDTO.java
│   │   ├── AverageResolutionTimeReportDTO.java
│   │   ├── ComplaintTypeDistributionDTO.java
│   │   └── DashboardOverviewDTO.java
│   └── repository/
│       └── ReportRepository.java       # Custom queries للتقارير
├── admin/
│   ├── service/
│   │   ├── AdminDashboardService.java  # Dashboard overview
│   │   └── AdminUserManagementService.java
│   └── controller/
│       └── AdminDashboardController.java
├── audit/
│   ├── entity/
│   │   └── AuditEvent.java
│   ├── service/
│   │   └── AuditService.java
│   ├── repository/
│   │   └── AuditEventRepository.java
│   └── controller/
│       └── AuditLogController.java
└── user/
    └── service/
        └── PasswordService.java        # تعديل كلمة السر
```

---

## 🎯 خطة التنفيذ التفصيلية

### المرحلة 1: البنية التحتية الأساسية (Infrastructure)

#### 1.1 إنشاء Audit System كامل

**الهدف**: نظام تسجيل كامل لجميع العمليات في النظام (ليس فقط الشكاوى)

**الاستراتيجية النهائية**: 
- ✅ **الاحتفاظ بـ `ComplaintHistory`** - مخصص للشكاوى فقط (موجود ومكتمل)
- ✅ **إضافة `AuditEvent`** - لتتبع جميع العمليات الأخرى في النظام:
  - إنشاء/تعديل/حذف المستخدمين والموظفين
  - تسجيل الدخول/الخروج
  - تغيير الصلاحيات
  - أي عمليات أخرى خارج نطاق الشكاوى
  - **ملاحظة**: يمكن أيضاً تسجيل عمليات الشكاوى في AuditEvent (لكن بشكل عام) بينما ComplaintHistory يسجل التفاصيل الدقيقة

**الفصل الواضح**:
- `ComplaintHistory`: Domain-specific للشكاوى - تفاصيل دقيقة (old/new values, actionDescription بالعربية) - للمستخدمين
- `AuditEvent`: System-wide للتدقيق - سجل عام (action, targetType, details JSON) - للمشرف العام

**المهام**:

1. **إنشاء AuditEvent Entity**:
```java
@Entity
@Table(name = "audit_events", indexes = {
    @Index(name = "idx_audit_actor", columnList = "actor_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_target", columnList = "target_type,target_id"),
    @Index(name = "idx_audit_created", columnList = "created_at DESC")
})
public class AuditEvent extends AuditedEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_event_seq")
    @SequenceGenerator(name = "audit_event_seq", sequenceName = "audit_event_id_seq", allocationSize = 1)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String action;              // CREATE_COMPLAINT, UPDATE_USER, LOGIN, etc.
    
    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;          // COMPLAINT, USER, EMPLOYEE, CITIZEN, etc.
    
    @Column(name = "target_id")
    private Long targetId;              // ID of the target entity
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;                  // User who performed the action
    
    @Column(nullable = false, length = 20)
    private String status;               // SUCCESS, FAILURE
    
    @Column(columnDefinition = "TEXT")
    private String details;              // JSON string with additional details
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    // createdAt is inherited from AuditedEntity
}
```

2. **إنشاء AuditService**:
```java
@Service
public class AuditService {
    public void record(String action, String targetType, Long targetId, 
                      String status, Map<String, Object> details);
    
    public PaginationDTO<AuditEventDTO> getAuditLogs(
        Long userId, String action, String targetType, 
        LocalDateTime from, LocalDateTime to, int page, int size);
    
    public void exportToCSV(OutputStream outputStream, ...);
    public void exportToPDF(OutputStream outputStream, ...);
}
```

3. **تحديث AuditAspect**:
```java
// تحديث AuditAspect لاستدعاء AuditService بدلاً من logging فقط
@Around("@annotation(audited)")
public Object auditSuccess(...) {
    // ...
    auditService.record(action, targetType, targetId, "SUCCESS", details);
}
```

**الملفات المطلوبة**:
- `src/main/java/com/Shakwa/audit/entity/AuditEvent.java`
- `src/main/java/com/Shakwa/audit/repository/AuditEventRepository.java`
- `src/main/java/com/Shakwa/audit/service/AuditService.java`
- `src/main/java/com/Shakwa/audit/controller/AuditLogController.java`
- `src/main/resources/db/migration/V6__create_audit_events.sql`

**المدة المتوقعة**: 3-4 أيام

---

#### 1.2 إنشاء Password Service

**الهدف**: إتاحة تعديل كلمة السر للمستخدمين

**المهام**:

1. **إنشاء PasswordService**:
```java
@Service
public class PasswordService {
    public void changePassword(Long userId, String oldPassword, String newPassword);
    public void resetPassword(Long userId, String newPassword); // Admin only
    public void requestPasswordReset(String email);
}
```

2. **إنشاء PasswordController**:
```java
@RestController
@RequestMapping("api/v1/password")
public class PasswordController {
    @PutMapping("/change")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest request);
    
    @PostMapping("/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@RequestBody PasswordResetRequest request);
}
```

**الملفات المطلوبة**:
- `src/main/java/com/Shakwa/user/service/PasswordService.java`
- `src/main/java/com/Shakwa/user/controller/PasswordController.java`
- `src/main/java/com/Shakwa/user/dto/ChangePasswordRequest.java`

**المدة المتوقعة**: 1-2 أيام

---

#### 1.3 إضافة البحث بالرقم المرجعي

**الهدف**: البحث عن شكوى بالرقم المرجعي

**المهام**:

1. **إضافة method في Repository**:
```java
// في ComplaintRepository
Optional<Complaint> findByTrackingNumber(String trackingNumber);
```

2. **إضافة method في Service**:
```java
// في ComplaintService
public ComplaintDTOResponse getComplaintByTrackingNumber(String trackingNumber);
```

3. **إضافة endpoint في Controller**:
```java
// في ComplaintController
@GetMapping("tracking/{trackingNumber}")
public ResponseEntity<ComplaintDTOResponse> getComplaintByTrackingNumber(
    @PathVariable String trackingNumber);
```

**الملفات المطلوبة**:
- تحديث `ComplaintRepository.java`
- تحديث `ComplaintService.java`
- تحديث `ComplaintController.java`

**المدة المتوقعة**: 0.5 يوم

---

### المرحلة 2: التقارير للموظف الحكومي (Employee Reports)

#### 2.1 تقرير إنجاز الشكاوى حسب الحالة

**الهدف**: عرض عدد الشكاوى حسب الحالة (منجزة، قيد المعالجة، جديدة، مرفوضة)

**التصميم**:

```java
// DTO
public class ComplaintStatusReportDTO {
    private Long totalComplaints;
    private Long resolvedCount;
    private Long inProgressCount;
    private Long pendingCount;
    private Long rejectedCount;
    private LocalDate fromDate;
    private LocalDate toDate;
    private GovernmentAgencyType agency; // null = all agencies
}

// Service Method
public ComplaintStatusReportDTO getComplaintStatusReport(
    GovernmentAgencyType agency, 
    LocalDate fromDate, 
    LocalDate toDate);
```

**SQL Query**:
```sql
SELECT 
    status,
    COUNT(*) as count
FROM complaints
WHERE government_agency = :agency
  AND created_at BETWEEN :fromDate AND :toDate
GROUP BY status;
```

**API Endpoint**:
```
GET /api/v1/reports/complaint-status
Query Params:
  - agency (optional)
  - fromDate (optional)
  - toDate (optional)
```

**الملفات المطلوبة**:
- `src/main/java/com/Shakwa/report/dto/ComplaintStatusReportDTO.java`
- `src/main/java/com/Shakwa/report/service/ReportService.java`
- `src/main/java/com/Shakwa/report/controller/ReportController.java`
- `src/main/java/com/Shakwa/report/repository/ReportRepository.java`

**المدة المتوقعة**: 2-3 أيام

---

#### 2.2 تقرير متوسط زمن الإغلاق

**الهدف**: حساب متوسط الوقت المستغرق لإنجاز الشكاوى

**التصميم**:

```java
// DTO
public class AverageResolutionTimeReportDTO {
    private Double averageDays;          // متوسط الأيام
    private Double averageHours;          // متوسط الساعات
    private Long totalResolvedComplaints;
    private Long minResolutionDays;
    private Long maxResolutionDays;
    private LocalDate fromDate;
    private LocalDate toDate;
    private GovernmentAgencyType agency;
}

// Service Method
public AverageResolutionTimeReportDTO getAverageResolutionTimeReport(
    GovernmentAgencyType agency,
    LocalDate fromDate,
    LocalDate toDate);
```

**SQL Query** (يمكن استخدام `ComplaintHistory` للحصول على دقة أكبر):
```sql
-- الطريقة 1: استخدام responded_at (بسيطة)
SELECT 
    AVG(EXTRACT(EPOCH FROM (responded_at - created_at)) / 86400) as avg_days,
    AVG(EXTRACT(EPOCH FROM (responded_at - created_at)) / 3600) as avg_hours,
    MIN(EXTRACT(EPOCH FROM (responded_at - created_at)) / 86400) as min_days,
    MAX(EXTRACT(EPOCH FROM (responded_at - created_at)) / 86400) as max_days,
    COUNT(*) as total_count
FROM complaints
WHERE government_agency = :agency
  AND status = 'RESOLVED'
  AND responded_at IS NOT NULL
  AND created_at BETWEEN :fromDate AND :toDate;

-- الطريقة 2: استخدام ComplaintHistory (أكثر دقة - يحدد وقت تغيير الحالة بالضبط)
SELECT 
    AVG(EXTRACT(EPOCH FROM (status_changed.created_at - c.created_at)) / 86400) as avg_days,
    AVG(EXTRACT(EPOCH FROM (status_changed.created_at - c.created_at)) / 3600) as avg_hours,
    MIN(EXTRACT(EPOCH FROM (status_changed.created_at - c.created_at)) / 86400) as min_days,
    MAX(EXTRACT(EPOCH FROM (status_changed.created_at - c.created_at)) / 86400) as max_days,
    COUNT(*) as total_count
FROM complaints c
INNER JOIN complaint_history status_changed 
    ON c.id = status_changed.complaint_id
    AND status_changed.action_type = 'STATUS_CHANGED'
    AND status_changed.new_value = 'RESOLVED'
WHERE c.government_agency = :agency
  AND c.status = 'RESOLVED'
  AND c.created_at BETWEEN :fromDate AND :toDate;
```

**ملاحظة**: يمكن استخدام `ComplaintHistory` للحصول على وقت تغيير الحالة بالضبط بدلاً من الاعتماد على `responded_at` فقط.

**API Endpoint**:
```
GET /api/v1/reports/average-resolution-time
Query Params:
  - agency (optional)
  - fromDate (optional)
  - toDate (optional)
```

**الملفات المطلوبة**:
- `src/main/java/com/Shakwa/report/dto/AverageResolutionTimeReportDTO.java`
- تحديث `ReportService.java`
- تحديث `ReportController.java`

**مثال على استخدام ComplaintHistory**:
```java
// في ReportService
@Autowired
private ComplaintHistoryRepository complaintHistoryRepository;

public AverageResolutionTimeReportDTO getAverageResolutionTimeReport(...) {
    // استخدام ComplaintHistory للحصول على وقت تغيير الحالة بالضبط
    List<ComplaintHistory> resolvedHistories = complaintHistoryRepository
        .findByComplaintIdAndActionTypeAndNewValue(
            complaintIds, 
            HistoryActionType.STATUS_CHANGED, 
            "RESOLVED"
        );
    
    // حساب متوسط الوقت من created_at إلى وقت تغيير الحالة
    double avgDays = resolvedHistories.stream()
        .mapToDouble(h -> ChronoUnit.DAYS.between(
            h.getComplaint().getCreatedAt(), 
            h.getCreatedAt()
        ))
        .average()
        .orElse(0.0);
    
    // ... باقي الحسابات
}
```

**المدة المتوقعة**: 2-3 أيام (يمكن تقليلها باستخدام ComplaintHistory الموجود)

---

#### 2.3 تقرير توزيع الشكاوى حسب النوع

**الهدف**: عرض توزيع الشكاوى حسب نوعها

**التصميم**:

```java
// DTO
public class ComplaintTypeDistributionDTO {
    private List<TypeCount> distribution;
    private Long totalComplaints;
    private LocalDate fromDate;
    private LocalDate toDate;
    private GovernmentAgencyType agency;
    
    @Data
    public static class TypeCount {
        private ComplaintType type;
        private Long count;
        private Double percentage;
    }
}

// Service Method
public ComplaintTypeDistributionDTO getComplaintTypeDistribution(
    GovernmentAgencyType agency,
    LocalDate fromDate,
    LocalDate toDate);
```

**SQL Query**:
```sql
SELECT 
    complaint_type,
    COUNT(*) as count,
    (COUNT(*) * 100.0 / (SELECT COUNT(*) FROM complaints 
                         WHERE government_agency = :agency 
                         AND created_at BETWEEN :fromDate AND :toDate)) as percentage
FROM complaints
WHERE government_agency = :agency
  AND created_at BETWEEN :fromDate AND :toDate
GROUP BY complaint_type
ORDER BY count DESC;
```

**API Endpoint**:
```
GET /api/v1/reports/complaint-type-distribution
Query Params:
  - agency (optional)
  - fromDate (optional)
  - toDate (optional)
```

**الملفات المطلوبة**:
- `src/main/java/com/Shakwa/report/dto/ComplaintTypeDistributionDTO.java`
- تحديث `ReportService.java`
- تحديث `ReportController.java`

**المدة المتوقعة**: 2-3 أيام

---

### المرحلة 3: لوحة مؤشرات الأداء للمشرف العام (Admin Dashboard)

#### 3.1 Dashboard Overview Service

**الهدف**: إحصائيات شاملة على مستوى النظام

**التصميم**:

```java
// DTO
public class DashboardOverviewDTO {
    // إجمالي الشكاوى
    private Long totalComplaints;
    private Long resolvedComplaints;
    private Long openComplaints;        // PENDING + IN_PROGRESS
    private Long rejectedComplaints;
    
    // أكثر الجهات استقبالاً
    private List<AgencyComplaintCount> topAgenciesByComplaints;
    
    // أكثر أنواع الشكاوى شيوعاً
    private List<TypeComplaintCount> topComplaintTypes;
    
    // متوسط زمن الإغلاق العام
    private Double averageResolutionTimeDays;
    
    // الشكاوى المتأخرة
    private Long overdueComplaints;
    private List<ComplaintSummaryDTO> overdueComplaintsList;
    
    // فترة الإحصائيات
    private LocalDate fromDate;
    private LocalDate toDate;
    
    @Data
    public static class AgencyComplaintCount {
        private GovernmentAgencyType agency;
        private Long complaintCount;
    }
    
    @Data
    public static class TypeComplaintCount {
        private ComplaintType type;
        private Long complaintCount;
    }
}
```

**Service Methods**:
```java
@Service
public class AdminDashboardService {
    private final ComplaintRepository complaintRepository;
    private final ComplaintHistoryRepository complaintHistoryRepository; // للاستفادة من ComplaintHistory
    
    public DashboardOverviewDTO getDashboardOverview(
        LocalDate fromDate, 
        LocalDate toDate);
    
    private List<AgencyComplaintCount> getTopAgenciesByComplaints(int limit);
    private List<TypeComplaintCount> getTopComplaintTypes(int limit);
    private Double getAverageResolutionTimeDays(); // يمكن استخدام ComplaintHistory للحصول على دقة أكبر
    private Long getOverdueComplaintsCount(int daysThreshold);
    private List<ComplaintSummaryDTO> getOverdueComplaints(int daysThreshold);
    
    // يمكن استخدام ComplaintHistory لتحديد الشكاوى المتأخرة بدقة أكبر
    private List<ComplaintSummaryDTO> getOverdueComplaintsUsingHistory(int daysThreshold);
}
```

**ملاحظة**: يمكن الاستفادة من `ComplaintHistory` الموجود لتحديد:
- وقت تغيير الحالة بالضبط (بدلاً من `responded_at`)
- الشكاوى المتأخرة بناءً على آخر نشاط
- متوسط زمن الإغلاق بدقة أكبر

**API Endpoint**:
```
GET /api/v1/admin/dashboard/overview
Query Params:
  - fromDate (optional, default: 30 days ago)
  - toDate (optional, default: today)
```

**الملفات المطلوبة**:
- `src/main/java/com/Shakwa/admin/service/AdminDashboardService.java`
- `src/main/java/com/Shakwa/admin/controller/AdminDashboardController.java`
- `src/main/java/com/Shakwa/admin/dto/DashboardOverviewDTO.java`

**المدة المتوقعة**: 3-4 أيام

---

#### 3.2 إدارة حسابات المستخدمين (Admin User Management)

**الهدف**: إدارة كاملة للمستخدمين والموظفين

**المهام**:

1. **توسيع EmployeeService**:
```java
// في EmployeeService
public void disableEmployee(Long employeeId);
public void enableEmployee(Long employeeId);
public void updateEmployeeRole(Long employeeId, String roleName);
public void updateEmployeeAgency(Long employeeId, GovernmentAgencyType agency);
```

2. **Citizen Management**:
```java
// في CitizenService
public void suspendCitizen(Long citizenId, String reason);
public void unsuspendCitizen(Long citizenId);
public void deleteCitizen(Long citizenId); // Soft delete
```

3. **Admin Controller**:
```java
@RestController
@RequestMapping("api/v1/admin/users")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AdminUserManagementController {
    @PutMapping("/employees/{id}/disable")
    public ResponseEntity<Void> disableEmployee(@PathVariable Long id);
    
    @PutMapping("/employees/{id}/enable")
    public ResponseEntity<Void> enableEmployee(@PathVariable Long id);
    
    @PutMapping("/citizens/{id}/suspend")
    public ResponseEntity<Void> suspendCitizen(@PathVariable Long id, @RequestBody SuspendRequest request);
    
    // ... المزيد
}
```

**الملفات المطلوبة**:
- تحديث `EmployeeService.java`
- تحديث `CitizenService.java`
- `src/main/java/com/Shakwa/admin/controller/AdminUserManagementController.java`

**المدة المتوقعة**: 2-3 أيام

---

### المرحلة 4: تصدير التقارير (Export Functionality)

#### 4.1 CSV Export

**الهدف**: تصدير التقارير بصيغة CSV

**التصميم**:

```java
@Service
public class ExportService {
    public void exportComplaintStatusReportToCSV(
        ComplaintStatusReportDTO report,
        OutputStream outputStream);
    
    public void exportAverageResolutionTimeToCSV(
        AverageResolutionTimeReportDTO report,
        OutputStream outputStream);
    
    public void exportComplaintTypeDistributionToCSV(
        ComplaintTypeDistributionDTO report,
        OutputStream outputStream);
    
    public void exportAuditLogToCSV(
        List<AuditEventDTO> auditEvents,
        OutputStream outputStream);
}
```

**API Endpoints**:
```
GET /api/v1/reports/complaint-status/export?format=csv
GET /api/v1/reports/average-resolution-time/export?format=csv
GET /api/v1/reports/complaint-type-distribution/export?format=csv
GET /api/v1/admin/audit-log/export?format=csv
```

**الملفات المطلوبة**:
- `src/main/java/com/Shakwa/report/service/ExportService.java`
- تحديث `ReportController.java`
- تحديث `AuditLogController.java`

**المدة المتوقعة**: 2-3 أيام

---

#### 4.2 PDF Export

**الهدف**: تصدير التقارير بصيغة PDF

**التصميم**:

**Dependencies** (في `pom.xml`):
```xml
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>
    <version>5.5.13.3</version>
</dependency>
```

```java
@Service
public class ExportService {
    public void exportComplaintStatusReportToPDF(
        ComplaintStatusReportDTO report,
        OutputStream outputStream);
    
    public void exportDashboardOverviewToPDF(
        DashboardOverviewDTO dashboard,
        OutputStream outputStream);
    
    // Helper methods
    private void addHeader(Document document, String title);
    private void addTable(Document document, List<String> headers, List<List<String>> data);
    private void addChart(Document document, byte[] chartImage); // Optional
}
```

**API Endpoints**:
```
GET /api/v1/reports/complaint-status/export?format=pdf
GET /api/v1/admin/dashboard/export?format=pdf
```

**الملفات المطلوبة**:
- تحديث `ExportService.java`
- تحديث Controllers

**المدة المتوقعة**: 3-4 أيام

---

### المرحلة 5: Audit Log Viewer

#### 5.1 Audit Log API

**الهدف**: عرض سجل التدقيق للمشرف العام

**التصميم**:

```java
@RestController
@RequestMapping("api/v1/admin/audit-log")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class AuditLogController {
    
    @GetMapping
    public ResponseEntity<PaginationDTO<AuditEventDTO>> getAuditLogs(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) String action,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size);
    
    @GetMapping("/export")
    public ResponseEntity<Resource> exportAuditLog(
        @RequestParam(required = false) String format, // csv or pdf
        // ... نفس filters
    );
}
```

**API Endpoint**:
```
GET /api/v1/admin/audit-log
Query Params:
  - userId (optional)
  - action (optional)
  - targetType (optional)
  - fromDate (optional)
  - toDate (optional)
  - page (default: 0)
  - size (default: 20)
```

**الملفات المطلوبة**:
- `src/main/java/com/Shakwa/audit/controller/AuditLogController.java`
- `src/main/java/com/Shakwa/audit/dto/AuditEventDTO.java`

**المدة المتوقعة**: 1-2 أيام

---

## 📊 جدول زمني مقترح

| المرحلة | المهام | المدة المتوقعة | الأولوية |
|---------|--------|----------------|----------|
| **المرحلة 1** | البنية التحتية (Audit, Password, Tracking Search) | 5-7 أيام | 🔴 عالية |
| **المرحلة 2** | تقارير الموظف (3 تقارير) | 6-9 أيام | 🔴 عالية |
| **المرحلة 3** | Dashboard للمشرف العام | 5-7 أيام | 🔴 عالية |
| **المرحلة 4** | تصدير التقارير (CSV/PDF) | 5-7 أيام | 🟡 متوسطة |
| **المرحلة 5** | Audit Log Viewer | 1-2 أيام | 🟡 متوسطة |
| **الإجمالي** | | **22-32 يوم عمل** | |

---

## 🔧 التفاصيل التقنية

### 1. Database Migrations

**V6__create_audit_events.sql**:
```sql
CREATE SEQUENCE IF NOT EXISTS audit_event_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS audit_events (
    id BIGINT PRIMARY KEY DEFAULT nextval('audit_event_id_seq'),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT,
    actor_id BIGINT,
    status VARCHAR(20) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE INDEX idx_audit_events_actor ON audit_events(actor_id);
CREATE INDEX idx_audit_events_action ON audit_events(action);
CREATE INDEX idx_audit_events_target ON audit_events(target_type, target_id);
CREATE INDEX idx_audit_events_created_at ON audit_events(created_at);
```

### 2. Dependencies المطلوبة

**في `pom.xml`**:
```xml
<!-- PDF Generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>
    <version>5.5.13.3</version>
</dependency>

<!-- CSV (يمكن استخدام Apache Commons CSV أو OpenCSV) -->
<dependency>
    <groupId>com.opencsv</groupId>
    <artifactId>opencsv</artifactId>
    <version>5.7.1</version>
</dependency>
```

### 3. Security & Authorization

**صلاحيات مطلوبة**:
```java
// في SystemRolesInitializer
createPermission("REPORT_VIEW", "View reports", "REPORT", "READ");
createPermission("REPORT_EXPORT", "Export reports", "REPORT", "EXPORT");
createPermission("ADMIN_DASHBOARD_VIEW", "View admin dashboard", "ADMIN", "READ");
createPermission("AUDIT_LOG_VIEW", "View audit log", "AUDIT", "READ");
createPermission("USER_MANAGE", "Manage users", "USER", "MANAGE");
```

**تحديث الأدوار**:
- `PLATFORM_ADMIN`: جميع الصلاحيات
- `SUPERVISOR`: `REPORT_VIEW`, `REPORT_EXPORT`, `ADMIN_DASHBOARD_VIEW` (لجهته فقط)
- `VIEWER`: `REPORT_VIEW` فقط

---

## ✅ Checklist للتنفيذ

### المرحلة 1: البنية التحتية
- [x] ✅ `ComplaintHistory` موجود ومكتمل (لا حاجة لإضافته)
- [ ] إنشاء `AuditEvent` entity (لجميع العمليات - ليس فقط الشكاوى)
- [ ] إنشاء `AuditEventRepository`
- [ ] إنشاء `AuditService`
- [ ] تحديث `AuditAspect` لاستدعاء `AuditService`
- [ ] Migration script للـ audit_events table
- [ ] إنشاء `PasswordService`
- [ ] إنشاء `PasswordController`
- [ ] إضافة `findByTrackingNumber()` في Repository
- [ ] إضافة endpoint للبحث بالرقم المرجعي

### المرحلة 2: تقارير الموظف
- [ ] إنشاء `ReportService`
- [ ] إنشاء `ReportRepository` مع custom queries
- [ ] تقرير إنجاز الشكاوى حسب الحالة
- [ ] تقرير متوسط زمن الإغلاق (يمكن استخدام `ComplaintHistory` لدقة أكبر)
- [ ] تقرير توزيع الشكاوى حسب النوع
- [ ] إنشاء `ReportController`
- [ ] **الاستفادة من `ComplaintHistory` الموجود** في التقارير المتعلقة بالشكاوى
- [ ] اختبار جميع التقارير

### المرحلة 3: Dashboard للمشرف
- [ ] إنشاء `AdminDashboardService`
- [ ] إنشاء `DashboardOverviewDTO`
- [ ] إحصائيات إجمالية
- [ ] أكثر الجهات استقبالاً
- [ ] أكثر أنواع الشكاوى شيوعاً
- [ ] متوسط زمن الإغلاق العام (**استخدام `ComplaintHistory` لدقة أكبر**)
- [ ] الشكاوى المتأخرة (**يمكن استخدام `ComplaintHistory` لتحديد آخر نشاط**)
- [ ] إنشاء `AdminDashboardController`
- [ ] **الاستفادة من `ComplaintHistoryRepository`** في Dashboard queries
- [ ] توسيع `EmployeeService` لإدارة الموظفين
- [ ] توسيع `CitizenService` لإدارة المواطنين
- [ ] إنشاء `AdminUserManagementController`

### المرحلة 4: التصدير
- [ ] إنشاء `ExportService`
- [ ] CSV export للتقارير
- [ ] PDF export للتقارير
- [ ] Export endpoints في Controllers
- [ ] اختبار التصدير

### المرحلة 5: Audit Log
- [ ] إنشاء `AuditLogController`
- [ ] API لعرض Audit Log
- [ ] Export Audit Log
- [ ] اختبار Audit Log viewer

---

## 🧪 استراتيجية الاختبار

### Unit Tests
- اختبار جميع Service methods
- اختبار SQL queries
- اختبار DTOs mapping

### Integration Tests
- اختبار API endpoints
- اختبار Authorization (صلاحيات)
- اختبار Export functionality

### Test Data
- إنشاء بيانات اختبار متنوعة
- شكاوى بحالات مختلفة
- شكاوى بفترات زمنية مختلفة
- مستخدمين بأدوار مختلفة

---

## 📝 ملاحظات مهمة

1. **الاستفادة من ComplaintHistory الموجود**:
   - ✅ `ComplaintHistory` موجود ومكتمل ويسجل جميع تغييرات الشكاوى
   - ✅ يمكن استخدامه في التقارير المتعلقة بالشكاوى للحصول على دقة أكبر:
     - **متوسط زمن الإغلاق**: استخدام `ComplaintHistory` لتحديد وقت تغيير الحالة إلى RESOLVED بالضبط
     - **الشكاوى المتأخرة**: استخدام `ComplaintHistory` لتحديد آخر نشاط على الشكوى
     - **تتبع التغييرات**: جميع التغييرات مسجلة بالفعل في `ComplaintHistory`
   - ⚠️ `AuditEvent` مطلوب فقط للعمليات **خارج نطاق الشكاوى** (مثل إدارة المستخدمين، تسجيل الدخول، إلخ)

2. **الأداء**:
   - استخدام Caching للتقارير (خاصة Dashboard Overview)
   - استخدام Indexes في قاعدة البيانات
   - Pagination لجميع القوائم
   - الاستفادة من `ComplaintHistory` الموجود بدلاً من إنشاء queries معقدة

3. **الأمان**:
   - التحقق من الصلاحيات في جميع endpoints
   - Rate limiting على endpoints الحساسة
   - Logging لجميع العمليات الحساسة

4. **البيانات الحساسة**:
   - عدم تضمين passwords في Audit Log
   - عدم تضمين معلومات حساسة في التقارير

5. **التوافق**:
   - دعم RTL للتقارير العربية
   - تنسيق التواريخ والأرقام حسب Locale

---

## 🚀 البدء بالتنفيذ

**الترتيب المقترح**:
1. المرحلة 1 (البنية التحتية) - أساسي لكل شيء
2. المرحلة 2 (تقارير الموظف) - مطلوب مباشرة
3. المرحلة 3 (Dashboard) - مطلوب للمشرف
4. المرحلة 4 (Export) - مكمل للتقارير
5. المرحلة 5 (Audit Log) - مكمل للنظام

---

## 📚 مراجع مفيدة

- [Spring Data JPA Custom Queries](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods)
- [iText PDF Library](https://itextpdf.com/)
- [OpenCSV Library](https://opencsv.sourceforge.net/)
- [Spring Security Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)

---

**تاريخ الإنشاء**: 2025
**آخر تحديث**: 2025

