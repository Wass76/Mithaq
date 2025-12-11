# Additional Information Request Feature - Analysis & Implementation Plan

## 📋 Executive Summary

**Feature Status:** ❌ **NOT IMPLEMENTED**

The system currently lacks the ability for employees to request additional information from citizens, and for citizens to provide that information in response.

---

## 🔍 Current State Analysis

### ✅ What EXISTS in the System:

1. **Complaint Management:**
   - Citizens can create complaints with attachments
   - Employees can update complaints, respond, and change status
   - Complaint history tracking system (versioning)
   - Notification system for status changes

2. **History Tracking:**
   - `HistoryActionType` enum tracks: CREATED, STATUS_CHANGED, UPDATED_FIELDS, ATTACHMENT_ADDED, ATTACHMENT_REMOVED, LOCKED, UNLOCKED
   - All changes are logged in `ComplaintHistory` table

3. **Notification System:**
   - Notifications for complaint creation
   - Notifications for status changes
   - Notifications for responses

4. **Attachment System:**
   - Citizens can add/delete attachments
   - Files are stored with metadata

### ❌ What is MISSING:

1. **No Request Mechanism:**
   - No way for employees to request additional information
   - No entity/table to store information requests
   - No status field to indicate "awaiting additional info"

2. **No Response Mechanism:**
   - No way for citizens to provide additional information in response to a request
   - No link between request and response

3. **No Tracking:**
   - No history action type for "INFO_REQUESTED" or "INFO_PROVIDED"
   - No notification type for "additional info requested"

4. **No Status Management:**
   - No complaint status like "AWAITING_INFO" or "INFO_REQUESTED"
   - No field to track pending information requests

---

## 🎯 Feature Requirements (from md.md)

From the project requirements document:

> **Section 4.4 - إدارة الشكاوى من قبل الجهات:**
> "يستطيع موظفو الجهات الحكومية عبر لوحة التحكم استعراض الشكاوى الخاصة بجهتهم، تعديل حالتها، إضافة ملاحظات، **أو طلب معلومات إضافية من المواطن**."

> **Section 3.3 - نظام الإشعارات:**
> "يرسل النظام إشعارات للمواطن عبر التطبيق عند استلام الشكوى، تغيير حالتها، **أو عند طلب معلومات إضافية**."

---

## 📐 System Architecture Design

### Option 1: Simple Approach (Recommended for MVP)
**Add a new field to Complaint entity + History tracking**

**Pros:**
- Simple to implement
- Quick to deliver
- Minimal database changes

**Cons:**
- Only one active request at a time
- Less flexible for complex scenarios

### Option 2: Separate Entity Approach (Recommended for Full Feature)
**Create new `InformationRequest` entity**

**Pros:**
- Multiple requests per complaint
- Better tracking and history
- More flexible and scalable
- Can track request status independently

**Cons:**
- More complex implementation
- More database tables
- More API endpoints

---

## 🏗️ Recommended Implementation Plan

### **Phase 1: Database Schema** (Week 1)

#### 1.1 Add Complaint Status (if needed)
```sql
-- Check if AWAITING_INFO status exists in ComplaintStatus enum
-- If not, add it to the enum
```

#### 1.2 Create InformationRequest Entity
```sql
CREATE TABLE information_requests (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaints(id) ON DELETE CASCADE,
    requested_by_id BIGINT NOT NULL REFERENCES users(id),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, RESPONDED, CANCELLED
    responded_at TIMESTAMP,
    response_message TEXT,
    response_attachments JSONB, -- Store attachment IDs
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_info_request_complaint ON information_requests(complaint_id);
CREATE INDEX idx_info_request_status ON information_requests(status);
CREATE INDEX idx_info_request_requested_by ON information_requests(requested_by_id);
```

#### 1.3 Add History Action Types
```java
// Add to HistoryActionType enum:
INFO_REQUESTED,      // Employee requested additional info
INFO_PROVIDED,       // Citizen provided additional info
INFO_REQUEST_CANCELLED  // Employee cancelled the request
```

### **Phase 2: Backend Implementation** (Week 1-2)

#### 2.1 Entity Classes
- `InformationRequest.java` - Main entity
- Update `ComplaintStatus` enum if needed

#### 2.2 Repository Layer
- `InformationRequestRepository.java`
- Query methods for finding requests by complaint, status, etc.

#### 2.3 Service Layer
- `InformationRequestService.java` with methods:
  - `requestAdditionalInfo(Long complaintId, String message)` - Employee creates request
  - `provideAdditionalInfo(Long requestId, String response, List<MultipartFile> files)` - Citizen responds
  - `cancelRequest(Long requestId)` - Employee cancels request
  - `getRequestsByComplaint(Long complaintId)` - Get all requests for a complaint
  - `getPendingRequests(Long complaintId)` - Get pending requests

#### 2.4 History Integration
- Update `ComplaintHistoryService`:
  - `recordInfoRequested(Complaint, Employee, String message)`
  - `recordInfoProvided(Complaint, Citizen, InformationRequest)`
  - `recordInfoRequestCancelled(Complaint, Employee, InformationRequest)`

#### 2.5 Notification Integration
- Update `ComplaintNotificationIntegration`:
  - `notifyInfoRequested(Complaint, InformationRequest)`
  - `notifyInfoProvided(Complaint, InformationRequest)` (optional - notify employee)

#### 2.6 DTOs
- `InformationRequestDTO.java` - Request/Response DTO
- `InformationRequestResponseDTO.java` - For listing requests
- Update `ComplaintDTOResponse` to include pending requests count

#### 2.7 Controller
- `InformationRequestController.java` with endpoints:
  - `POST /api/v1/complaints/{id}/info-requests` - Employee creates request
  - `PUT /api/v1/info-requests/{id}/respond` - Citizen responds
  - `DELETE /api/v1/info-requests/{id}` - Employee cancels request
  - `GET /api/v1/complaints/{id}/info-requests` - Get all requests
  - `GET /api/v1/info-requests/{id}` - Get single request

### **Phase 3: Business Logic & Security** (Week 2)

#### 3.1 Authorization Rules
- ✅ Only employees from the same agency can request info
- ✅ Only the complaint owner (citizen) can respond
- ✅ Only the requester or admin can cancel
- ✅ Employees can view all requests for their agency's complaints
- ✅ Citizens can only view requests for their own complaints

#### 3.2 Validation
- Request message cannot be empty
- Response message cannot be empty (if no files provided)
- At least one of: response message OR files must be provided
- File validation (size, type, count)

#### 3.3 Status Management
- When request is created: Optionally set complaint status to "AWAITING_INFO"
- When request is responded: Optionally change complaint status back to "PENDING" or "IN_PROGRESS"
- Track request status independently

### **Phase 4: Frontend Integration** (Week 3)

#### 4.1 Employee Dashboard
- Button/UI to request additional info
- Modal/form to enter request message
- Display pending requests for each complaint
- View citizen responses

#### 4.2 Citizen Mobile App
- Notification when info is requested
- UI to view request details
- Form to provide response (text + files)
- Display request history

### **Phase 5: Testing** (Week 3-4)

#### 5.1 Unit Tests
- Service layer tests
- Repository tests
- Validation tests

#### 5.2 Integration Tests
- API endpoint tests
- Authorization tests
- Notification tests
- History tracking tests

#### 5.3 End-to-End Tests
- Complete flow: Request → Notification → Response → History

---

## 📊 Database Schema Details

### InformationRequest Table
```sql
CREATE TABLE information_requests (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL,
    requested_by_id BIGINT NOT NULL,
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    responded_at TIMESTAMP,
    response_message TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_info_request_complaint 
        FOREIGN KEY (complaint_id) REFERENCES complaints(id) ON DELETE CASCADE,
    CONSTRAINT fk_info_request_requester 
        FOREIGN KEY (requested_by_id) REFERENCES users(id),
    CONSTRAINT chk_info_request_status 
        CHECK (status IN ('PENDING', 'RESPONDED', 'CANCELLED'))
);

-- Indexes
CREATE INDEX idx_info_request_complaint ON information_requests(complaint_id);
CREATE INDEX idx_info_request_status ON information_requests(status);
CREATE INDEX idx_info_request_requested_by ON information_requests(requested_by_id);
CREATE INDEX idx_info_request_complaint_status ON information_requests(complaint_id, status);
```

### InformationRequestAttachment Table (if separate)
```sql
CREATE TABLE information_request_attachments (
    id BIGSERIAL PRIMARY KEY,
    information_request_id BIGINT NOT NULL,
    attachment_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_info_req_att_request 
        FOREIGN KEY (information_request_id) REFERENCES information_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_info_req_att_attachment 
        FOREIGN KEY (attachment_id) REFERENCES complaint_attachments(id) ON DELETE CASCADE,
    CONSTRAINT uk_info_req_att UNIQUE (information_request_id, attachment_id)
);
```

---

## 🔄 User Flows

### Flow 1: Employee Requests Additional Info
1. Employee opens complaint in dashboard
2. Clicks "Request Additional Information"
3. Enters request message (required)
4. Optionally selects specific fields they need (location, description, etc.)
5. Submits request
6. System:
   - Creates `InformationRequest` record
   - Records in `ComplaintHistory`
   - Sends notification to citizen
   - Optionally changes complaint status to "AWAITING_INFO"

### Flow 2: Citizen Provides Additional Info
1. Citizen receives notification
2. Opens complaint in mobile app
3. Sees pending information request
4. Clicks "Provide Information"
5. Enters response message
6. Optionally uploads additional files
7. Submits response
8. System:
   - Updates `InformationRequest` status to "RESPONDED"
   - Records in `ComplaintHistory`
   - Links attachments to request
   - Sends notification to employee (optional)
   - Optionally changes complaint status back

### Flow 3: Employee Cancels Request
1. Employee views pending request
2. Clicks "Cancel Request"
3. System:
   - Updates request status to "CANCELLED"
   - Records in history
   - Sends notification to citizen (optional)

---

## 🎨 API Design

### Request Additional Information
```http
POST /api/v1/complaints/{complaintId}/info-requests
Authorization: Bearer {employee_token}
Content-Type: application/json

{
  "message": "نحتاج إلى صورة واضحة للوثيقة المرفقة",
  "requestedFields": ["attachments", "location"] // Optional
}

Response: 201 Created
{
  "id": 1,
  "complaintId": 123,
  "requestedBy": {
    "id": 5,
    "name": "أحمد محمد"
  },
  "requestMessage": "نحتاج إلى صورة واضحة للوثيقة المرفقة",
  "status": "PENDING",
  "requestedAt": "2025-01-15T10:30:00"
}
```

### Provide Additional Information
```http
PUT /api/v1/info-requests/{requestId}/respond
Authorization: Bearer {citizen_token}
Content-Type: multipart/form-data

responseMessage: "إليكم الصورة المطلوبة"
files: [file1.jpg, file2.pdf]

Response: 200 OK
{
  "id": 1,
  "status": "RESPONDED",
  "responseMessage": "إليكم الصورة المطلوبة",
  "respondedAt": "2025-01-15T14:20:00",
  "attachments": [...]
}
```

### Get Information Requests
```http
GET /api/v1/complaints/{complaintId}/info-requests
Authorization: Bearer {token}

Response: 200 OK
{
  "content": [
    {
      "id": 1,
      "requestMessage": "...",
      "status": "RESPONDED",
      "responseMessage": "...",
      "requestedAt": "...",
      "respondedAt": "..."
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

## 📝 Implementation Checklist

### Backend
- [ ] Create database migration for `information_requests` table
- [ ] Create `InformationRequest` entity
- [ ] Create `InformationRequestRepository`
- [ ] Create `InformationRequestService`
- [ ] Add new `HistoryActionType` values
- [ ] Update `ComplaintHistoryService` with new methods
- [ ] Update `ComplaintNotificationIntegration` with new notification types
- [ ] Create DTOs (`InformationRequestDTO`, etc.)
- [ ] Create `InformationRequestController`
- [ ] Add authorization checks
- [ ] Add validation
- [ ] Write unit tests
- [ ] Write integration tests

### Frontend (Future)
- [ ] Employee dashboard: Request info UI
- [ ] Employee dashboard: View responses UI
- [ ] Citizen app: View requests UI
- [ ] Citizen app: Provide response UI
- [ ] Notification handling

### Documentation
- [ ] API documentation (Swagger)
- [ ] User guide
- [ ] Testing guide

---

## 🚀 Quick Start Implementation (MVP)

For a minimal viable product, you can start with:

1. **Simple approach**: Add a `pendingInfoRequest` field to `Complaint` entity
2. **Single request**: One active request per complaint
3. **Basic flow**: Request → Response → Done
4. **History tracking**: Record in `ComplaintHistory`
5. **Notifications**: Send when request is created

This can be expanded later to the full entity-based approach.

---

## 📈 Success Metrics

- Employees can successfully request additional information
- Citizens receive notifications and can respond
- All requests and responses are tracked in history
- System maintains data integrity and security
- API response times remain acceptable

---

## 🔐 Security Considerations

1. **Authorization**: Strict role-based access control
2. **Validation**: Input sanitization and file validation
3. **Rate Limiting**: Prevent abuse of request creation
4. **Audit Trail**: All actions logged in history
5. **Data Privacy**: Citizens can only see their own requests

---

## 📚 Related Files to Modify

### New Files:
- `InformationRequest.java` (entity)
- `InformationRequestRepository.java`
- `InformationRequestService.java`
- `InformationRequestController.java`
- `InformationRequestDTO.java`
- Migration file: `V4__add_information_requests.sql`

### Files to Update:
- `HistoryActionType.java` - Add new enum values
- `ComplaintHistoryService.java` - Add new recording methods
- `ComplaintNotificationIntegration.java` - Add notification methods
- `ComplaintService.java` - Optionally integrate with complaint status
- `ComplaintDTOResponse.java` - Add pending requests info

---

## ⏱️ Estimated Timeline

- **Phase 1 (Database)**: 1-2 days
- **Phase 2 (Backend Core)**: 3-5 days
- **Phase 3 (Business Logic)**: 2-3 days
- **Phase 4 (Frontend)**: 5-7 days (if doing frontend)
- **Phase 5 (Testing)**: 2-3 days

**Total Backend Implementation**: ~2 weeks
**Total with Frontend**: ~3-4 weeks

---

## 🎯 Next Steps

1. **Review this plan** with the team
2. **Choose approach**: Simple field-based or full entity-based
3. **Create database migration**
4. **Implement backend services**
5. **Add tests**
6. **Integrate with frontend** (when ready)

---

*Last Updated: 2025-01-15*
*Document Version: 1.0*

