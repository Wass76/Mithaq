# دليل اختبار Login باستخدام JMeter - بدون Registration

## السؤال: هل يمكن اختبار Login بدون Registration؟

**الإجابة: نعم! ✅** هناك عدة طرق:

---

## الخيار 1: استخدام Admin Users الموجودين مسبقاً (الأسهل) ⭐

### معلومات المستخدمين الجاهزين:

نظامك يقوم بإنشاء مستخدمين Admin تلقائياً عند بدء التطبيق. يمكنك استخدامهم مباشرة!

#### Admin Users المتاحة:

**1. Super Admin:**
- **Email:** `super.admin@mithaq.com`
- **Password:** `Password!1`
- **Endpoint:** `POST /api/v1/admin/login`

**2. Platform Admin:**
- **Email:** `admin@mithaq.com`
- **Password:** `Password!1`
- **Endpoint:** `POST /api/v1/admin/login`

### ✅ المزايا:
- جاهزين للاستخدام مباشرة
- لا يحتاجون إلى Registration أو OTP
- مثالي لبدء الاختبارات السريعة

### ⚠️ ملاحظات:
- هؤلاء مستخدمون Admin فقط
- إذا أردت اختبار Citizen Login، تحتاج الخيار 2 أو 3

---

## الخيار 2: إنشاء Citizen User يدوياً في قاعدة البيانات

إذا كنت تريد اختبار Citizen Login، يمكنك:

1. **استخدام Admin User لإنشاء Citizen:**
   - Login كـ Admin أولاً
   - استخدام endpoint: `POST /api/v1/citizens` (Create Citizen)
   - هذا يتخطى عملية OTP

2. **أو إدراج بيانات مباشرة في قاعدة البيانات:**
   ```sql
   -- مثال: إنشاء Citizen مباشرة (يتطلب معرفة كلمة المرور المشفرة)
   INSERT INTO citizens (first_name, last_name, email, password, status, ...) 
   VALUES ('Test', 'User', 'test@example.com', '$2a$10$...', 'ACTIVE', ...);
   ```

---

## الخيار 3: استخدام JMeter لعمل Registration + Verify OTP ثم Login (الأكثر واقعية)

هذا الخيار يختبر العملية الكاملة كما يحدث في الواقع.

### الخطوات:

#### 3.1. Setup Thread Group
1. Add → Threads (Users) → Thread Group
2. Number of Threads: `1` (للاختبار الأولي)

#### 3.2. HTTP Request Defaults
1. Add → Config Element → HTTP Request Defaults
2. Server Name: `localhost`
3. Port: `13000`

#### 3.3. Step 1: Register Citizen
1. Add → Sampler → HTTP Request
2. Name: `Register Citizen`
3. Method: `POST`
4. Path: `/api/v1/citizens/register`
5. Body Data:
```json
{
  "firstName": "Test",
  "lastName": "User",
  "email": "test${__threadNum}@example.com",
  "password": "Password123!"
}
```

#### 3.4. Step 2: استخدام OTP ثابت للاختبار (الموصى به للتجربة السريعة) ⭐

**المنطق الحالي:** OTP يُرسل عبر الإيميل ويُحفظ في قاعدة البيانات.

**للاختبار فقط (بدون تعديل كود التطبيق):** يمكنك استخدام OTP ثابت في JMeter.

##### الطريقة 1: استخدام User Defined Variables (الأبسط)

1. **في Test Plan Level:**
   - Right Click على "Test Plan"
   - Add → Config Element → User Defined Variables
   - Add Variable:
     - Name: `otpCode`
     - Value: `123456` (أو أي OTP تريده للاختبار)

2. **الاستخدام في Verify OTP Request:**
   - Variable `${otpCode}` سيكون متاح في كل Requests

**ملاحظة مهمة:** هذه الطريقة تعمل فقط إذا قمت بتعديل قاعدة البيانات يدوياً لحفظ نفس OTP، أو إذا كنت تختبر مع OTP معروف مسبقاً.

##### الطريقة 2: استخدام CSV Data Set Config مع OTP ثابت (الأفضل)

1. **إنشاء/تعديل ملف CSV:**
```csv
firstName,lastName,email,password,otpCode
أحمد,محمد,user1@test.com,Password123!,123456
علي,حسن,user2@test.com,Password123!,123456
فاطمة,علي,user3@test.com,Password123!,123456
```

2. **في CSV Data Set Config:**
   - Variable names: `firstName,lastName,email,password,otpCode`

3. **الاستخدام في Verify OTP Request:**
   - Variable `${otpCode}` سيكون متاح من CSV

##### الطريقة 3: استخدام OTP من قاعدة البيانات (الواقعية) - راجع JMeter_Registration_Load_Test_Guide.md

#### 3.5. Step 3: Verify OTP Request

1. Add → Sampler → HTTP Request
2. Name: `Verify OTP`
3. Method: `POST`
4. Path: `/api/v1/citizens/verify-otp`
5. Body Data:
```json
{
  "email": "${email}",
  "otpCode": "${otpCode}"
}
```

**ملاحظة:** استخدم `${email}` من CSV Data Set Config، و`${otpCode}` من User Defined Variables أو CSV.

#### 3.6. Step 4: Login
1. Add → Sampler → HTTP Request
2. Name: `Login Citizen`
3. Method: `POST`
4. Path: `/api/v1/citizens/login`
5. Body Data:
```json
{
  "email": "test${__threadNum}@example.com",
  "password": "Password123!"
}
```

#### 3.7. Extract JWT Token
1. Add → Post Processors → JSON Extractor
2. Variable name: `jwtToken`
3. JSON Path: `$.token`

---

## الخيار 4: استخدام مستخدمين موجودين بالفعل

إذا كان لديك مستخدمين موجودين في قاعدة البيانات:

1. **Citizen Users:** يجب أن يكونوا verified (لا يوجد OTP pending)
2. **Employee Users:** يجب أن يكونوا ACTIVE
3. **Admin Users:** جاهزين دائماً

---

## ✅ التوصية: ابدأ بالخيار 1 (Admin Login)

### خطوات سريعة لاختبار Admin Login:

#### 1. Thread Group
- Number of Threads: `10`
- Ramp-up period: `5`
- Loop Count: `5`

#### 2. HTTP Request Defaults
- Server: `localhost`
- Port: `13000`

#### 3. Login Request
- Name: `Admin Login`
- Method: `POST`
- Path: `/api/v1/admin/login`
- Body Data:
```json
{
  "email": "admin@mithaq.com",
  "password": "Password!1"
}
```

#### 4. JSON Extractor (لاستخراج Token)
- Variable name: `jwtToken`
- JSON Path: `$.token`

#### 5. Listeners
- View Results Tree
- Summary Report

#### 6. Run Test

---

## 📝 مثال كامل لـ Test Plan (Admin Login)

```
Test Plan
└── Thread Group (10 users, 5 ramp-up, 5 loops)
    ├── HTTP Request Defaults (localhost:13000)
    ├── HTTP Cookie Manager
    ├── Admin Login Request
    │   └── JSON Extractor (jwtToken = $.token)
    ├── HTTP Header Manager (Authorization: Bearer ${jwtToken})
    ├── Get Complaints Request (اختبار استخدام Token)
    └── Listeners
        ├── View Results Tree
        └── Summary Report
```

---

## 🔄 استخدام CSV Data Set Config لعدة مستخدمين

إذا أردت اختبار Login بعدة Admin users:

### 1. إنشاء ملف CSV: `admin-users.csv`
```csv
email,password
admin@mithaq.com,Password!1
super.admin@mithaq.com,Password!1
```

### 2. في JMeter:
1. Add → Config Element → CSV Data Set Config
2. Filename: `path/to/admin-users.csv`
3. Variable names: `email,password`
4. Delimiter: `,`

### 3. في Login Request Body:
```json
{
  "email": "${email}",
  "password": "${password}"
}
```

---

## ⚠️ ملاحظات مهمة

### للـ Citizen Login:
- **يجب** أن يكون User verified (تم التحقق من OTP)
- إذا كان هناك OTP pending، Login سيفشل مع رسالة: "Please verify your email first"

### للـ Employee Login:
- **يجب** أن يكون Employee ACTIVE
- Employees يتم إنشاؤهم من قبل Admin فقط

### Rate Limiting:
- النظام يستخدم Rate Limiting
- إذا كانت محاولات Login كثيرة جداً، قد يتم حظر IP
- استخدم Ramp-up period للزيادة التدريجية

---

## 🎯 الخطوات التوصى بها للبدء

### الأسبوع الأول:
1. ✅ اختبار Admin Login (الخيار 1) - أسهل وأسرع
2. ✅ اختبار استخراج JWT Token
3. ✅ اختبار استخدام Token في API calls أخرى

### الأسبوع الثاني:
1. ✅ اختبار Citizen Registration + OTP Verification + Login (الخيار 3)
2. ✅ اختبار Employee Login (إذا كان لديك Employees موجودين)

---

## 📚 المراجع

- راجع `JMeter_Testing_Guide.md` للدليل الكامل
- راجع `JMeter_Quick_Start.md` للخطوات الأساسية

---

## الخلاصة

**نعم، يمكنك اختبار Login بدون Registration!**

- ✅ **للـ Admin:** استخدم `admin@mithaq.com` / `Password!1` مباشرة
- ✅ **للـ Citizen:** استخدم JMeter لعمل Register + Verify OTP + Login
- ✅ **للـ Employee:** يجب أن يكون موجود مسبقاً (يُنشأ من Admin)

**ابدأ بـ Admin Login - إنه الأسهل! 🚀**

