# اختبار API باستخدام Admin Token مباشرة (بدون Login Request)

## الهدف

اختبار Endpoints (مثل GET Complaints) باستخدام Admin Token مباشرة بدون عمل Login Request في كل مرة.

---

## متى تستخدم هذه الطريقة؟

- ✅ عندما تريد اختبار API مباشرة بدون Login في كل iteration
- ✅ عندما تريد اختبار أداء API فقط (بدون تضمين وقت Login)
- ✅ عندما يكون لديك Token ثابت أو من session سابق
- ✅ لتسريع الاختبارات

---

## الطريقة 1: استخدام User Defined Variables (Token ثابت)

### الخطوات:

#### 1. الحصول على Admin Token (مرة واحدة)

**الطريقة A: استخدام Postman/Browser:**
1. Login كـ Admin:
   - POST `/api/v1/admin/login`
   - Body: `{"email": "admin@mithaq.com", "password": "Password!1"}`
2. نسخ Token من Response

**الطريقة B: استخدام JMeter (مرة واحدة):**
1. إنشاء Test Plan بسيط:
   - Thread Group (1 user)
   - Admin Login Request
   - JSON Extractor (jwtToken)
   - View Results Tree
2. تشغيل Test Plan
3. نسخ Token من Response أو Logs

#### 2. وضع Token في JMeter

**في Test Plan Level:**
1. Right Click على **Test Plan**
2. Add → Config Element → **User Defined Variables**
3. Add Variable:
   - **Name:** `adminToken`
   - **Value:** `eyJhbGciOiJIUzI1NiJ9...` (الـ Token الكامل)

#### 3. استخدام Token في Requests

**HTTP Header Manager:**
1. Add → Config Element → HTTP Header Manager
2. Add Header:
   - **Name:** `Authorization`
   - **Value:** `Bearer ${adminToken}`

**أو في كل Request مباشرة:**
- في HTTP Request → Headers tab
- Add Header:
  - Name: `Authorization`
  - Value: `Bearer ${adminToken}`

---

## الطريقة 2: استخدام Setup Thread Group (للحصول على Token تلقائياً)

### الفكرة:
استخدام Setup Thread Group للحصول على Token مرة واحدة في البداية، ثم استخدامه في Thread Group الرئيسي.

### الخطوات:

#### 1. Setup Thread Group

1. Add → Threads (Users) → **Setup Thread Group**
2. Configuration:
   - Number of Threads: `1`
   - Ramp-up: `1`

#### 2. Admin Login Request

1. Add → Sampler → HTTP Request
2. Name: `Admin Login (Setup)`
3. Configuration:
   - Method: `POST`
   - Path: `/api/v1/admin/login`
   - Body Data:
```json
{
  "email": "admin@mithaq.com",
  "password": "Password!1"
}
```

#### 3. JSON Extractor

1. Add → Post Processors → JSON Extractor
2. Configuration:
   - Variable names: `adminToken`
   - JSON Path Expressions: `$.token`
   - Scope: `All threads` (مهم!)

#### 4. Thread Group الرئيسي

1. Add → Threads (Users) → Thread Group
2. Number of Threads: `100` (مثلاً)
3. Add → Config Element → HTTP Header Manager
   - Authorization: `Bearer ${adminToken}`

#### 5. GET Complaints Request

1. Add → Sampler → HTTP Request
2. Name: `Get Complaints`
3. Configuration:
   - Method: `GET`
   - Path: `/api/v1/complaints?page=0&size=10`

**الـ Token سيستخدم تلقائياً من HTTP Header Manager!**

---

## مثال كامل: GET Complaints باستخدام Admin Token

### Test Plan Structure:

```
Test Plan
├── HTTP Request Defaults (localhost:13000)
├── Setup Thread Group (للحصول على Token)
│   ├── Admin Login Request
│   └── JSON Extractor (adminToken = $.token)
├── Thread Group (الاختبار الرئيسي)
│   ├── HTTP Header Manager (Authorization: Bearer ${adminToken})
│   ├── Get Complaints Request
│   └── Listeners
```

### الخطوات التفصيلية:

#### الخطوة 1: HTTP Request Defaults
- Server Name: `localhost`
- Port Number: `13000`

#### الخطوة 2: Setup Thread Group
1. Add → Threads (Users) → Setup Thread Group
2. Number of Threads: `1`
3. Ramp-up: `1`

#### الخطوة 3: Admin Login في Setup Thread Group
1. Add → Sampler → HTTP Request
2. Name: `Admin Login`
3. Method: `POST`
4. Path: `/api/v1/admin/login`
5. Body Data:
```json
{
  "email": "admin@mithaq.com",
  "password": "Password!1"
}
```

#### الخطوة 4: JSON Extractor (في Setup Thread Group)
1. Add → Post Processors → JSON Extractor
2. Variable names: `adminToken`
3. JSON Path Expressions: `$.token`
4. Match No.: `1`

#### الخطوة 5: Thread Group الرئيسي
1. Add → Threads (Users) → Thread Group
2. Number of Threads: `50`
3. Ramp-up period: `10`
4. Loop Count: `5`

#### الخطوة 6: HTTP Header Manager (في Thread Group)
1. Add → Config Element → HTTP Header Manager
2. Add Header:
   - Name: `Authorization`
   - Value: `Bearer ${adminToken}`

#### الخطوة 7: Get Complaints Request
1. Add → Sampler → HTTP Request
2. Name: `Get Complaints`
3. Method: `GET`
4. Path: `/api/v1/complaints?page=0&size=10`

#### الخطوة 8: Listeners
1. Add → Listener → Summary Report
2. Add → Listener → Aggregate Report
3. (اختياري) View Results Tree (للاختبار الأولي فقط)

---

## مقارنة الطريقتين

| الميزة | User Defined Variables | Setup Thread Group |
|--------|----------------------|-------------------|
| **البساطة** | ⭐⭐⭐⭐⭐ أبسط | ⭐⭐⭐ معقد قليلاً |
| **Token تلقائي** | ❌ يدوي | ✅ تلقائي |
| **Token منتهي** | ⚠️ قد ينتهي | ✅ جديد في كل run |
| **الاستخدام** | Token ثابت/طويل الأمد | Token ديناميكي |

---

## مثال Body Data للـ Requests

### Admin Login Request:
```json
{
  "email": "admin@mithaq.com",
  "password": "Password!1"
}
```

### Get Complaints Request:
**لا يحتاج Body Data** - GET Request فقط
- Path: `/api/v1/complaints?page=0&size=10`
- Headers: `Authorization: Bearer ${adminToken}`

---

## نصائح مهمة

### ✅ يجب فعله:
1. **استخدم Setup Thread Group** إذا كنت تريد Token جديد في كل run
2. **استخدم User Defined Variables** إذا كان لديك Token طويل الأمد
3. **تأكد من Scope في JSON Extractor** - استخدم `All threads`
4. **اختبر Token أولاً** - تأكد أن Token يعمل قبل الاختبار الكبير

### ❌ يجب تجنبه:
1. **لا تضع Token في Code** - استخدم Variables
2. **لا تستخدم Token منتهي** - تحقق من صلاحيته
3. **لا تنسَ HTTP Header Manager** - بدونها لن يعمل Authentication

---

## حل المشاكل الشائعة

### المشكلة: 401 Unauthorized
**السبب:** Token غير صالح أو منتهي أو غير موجود
**الحل:**
- تحقق من قيمة `${adminToken}` في View Results Tree
- تأكد من HTTP Header Manager موجود ومفعّل
- جرب Token جديد

### المشكلة: Token لا يعمل
**السبب:** JSON Extractor لم يستخرج Token بشكل صحيح
**الحل:**
- تحقق من JSON Path: `$.token`
- تأكد من Response Structure
- استخدم View Results Tree لرؤية Response

### المشكلة: Token غير متاح في Thread Group الرئيسي
**السبب:** Scope في JSON Extractor غير صحيح
**الحل:**
- ضع JSON Extractor في Setup Thread Group
- استخدم Scope: `All threads`
- أو ضع Variable في Test Plan level

---

## الخلاصة

لاختبار GET Complaints (أو أي API) باستخدام Admin Token مباشرة:

1. ✅ **الطريقة السريعة:** User Defined Variables + Token ثابت
2. ✅ **الطريقة الأفضل:** Setup Thread Group + Token تلقائي
3. ✅ **تأكد من:** HTTP Header Manager مع `Authorization: Bearer ${adminToken}`
4. ✅ **GET Complaints:** `/api/v1/complaints?page=0&size=10`

**الطريقة الموصى بها:** Setup Thread Group - يعطيك Token جديد في كل run! 🚀

