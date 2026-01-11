# دليل اختبار Registration + OTP لعدد كبير من المستخدمين

## الهدف

اختبار عملية Registration مع OTP لعدد كبير من المستخدمين المتزامنين باستخدام JMeter.

---

## نظرة عامة على العملية

1. **Registration** → إنشاء حساب جديد + إنشاء OTP
2. **Get OTP** → جلب OTP من قاعدة البيانات
3. **Verify OTP** → التحقق من OTP وتفعيل الحساب
4. **Login** → (اختياري) تسجيل الدخول بعد التفعيل

---

## التحدي: كيفية الحصول على OTP؟

OTP **لا يُرجع في Response** - يتم إرساله بالإيميل أو طباعته في Logs فقط.

### الحلول المتاحة:

#### ✅ الحل 1: استخدام JDBC Request (الأفضل للأتمتة)
- جلب OTP مباشرة من قاعدة البيانات بعد Registration
- يعمل بشكل ممتاز مع عدد كبير من المستخدمين
- يتطلب إعداد JDBC Connection في JMeter

#### ✅ الحل 2: قراءة OTP من Logs (للتطوير فقط)
- OTP مطبوع في Application Logs
- غير مناسب للاختبارات الآلية الكبيرة

#### ✅ الحل 3: استخدام OTP ثابت للتطوير
- تعديل كود التطبيق مؤقتاً لإرجاع OTP في Response (للتطوير فقط)

---

## الحل الموصى به: JDBC Request

سنستخدم JDBC Request لجلب OTP من قاعدة البيانات مباشرة بعد Registration.

---

## المتطلبات الأساسية

### 1. تثبيت JDBC Driver لـ PostgreSQL في JMeter

1. تحميل PostgreSQL JDBC Driver:
   - من: https://jdbc.postgresql.org/download/
   - تحميل أحدث إصدار (مثلاً: `postgresql-42.7.1.jar`)

2. وضع الملف في JMeter:
   - نسخ `postgresql-42.7.1.jar` إلى مجلد `lib/` في JMeter
   - أو إلى `lib/ext/` في JMeter

### 2. معلومات الاتصال بقاعدة البيانات

من `application.properties`:
- **Host:** `localhost`
- **Port:** `5432`
- **Database:** `mithaq`
- **Username:** `postgres`
- **Password:** `password`
- **JDBC URL:** `jdbc:postgresql://localhost:5432/mithaq`

---

## خطوات إنشاء Test Plan

### الخطوة 1: Setup Test Plan

1. إنشاء Test Plan جديد
2. Add → Config Element → JDBC Connection Configuration
   - Variable Name: `postgresDB`
   - Database URL: `jdbc:postgresql://localhost:5432/mithaq`
   - JDBC Driver class: `org.postgresql.Driver`
   - Username: `postgres`
   - Password: `password`

### الخطوة 2: Thread Group

1. Add → Threads (Users) → Thread Group
2. Configuration:
   - **Number of Threads:** `100` (عدد المستخدمين المتزامنين)
   - **Ramp-up period:** `60` (60 ثانية لزيادة العدد تدريجياً)
   - **Loop Count:** `1` (كل مستخدم يسجل مرة واحدة)
   - **Scheduler:** (اختياري) تحديد مدة الاختبار

### الخطوة 3: HTTP Request Defaults

1. Add → Config Element → HTTP Request Defaults
2. Configuration:
   - Server Name: `localhost`
   - Port Number: `13000`

### الخطوة 4: CSV Data Set Config

1. إنشاء ملف CSV: `registration-users.csv`

```csv
firstName,lastName,email,password
أحمد,محمد,ahmed1@test.com,Password123!
علي,حسن,ali1@test.com,Password123!
فاطمة,علي,fatima1@test.com,Password123!
محمد,خالد,mohammed1@test.com,Password123!
سارة,أحمد,sara1@test.com,Password123!
```

**ملاحظة:** استخدم أرقام فريدة في Email مثل: `user${__threadNum}@test.com`

2. في JMeter:
   - Add → Config Element → CSV Data Set Config
   - Filename: `path/to/registration-users.csv`
   - Variable names: `firstName,lastName,email,password`
   - Delimiter: `,`
   - Recycle on EOF: `True` (إعادة استخدام البيانات)
   - Stop thread on EOF: `False`
   - Sharing mode: `All threads`

### الخطوة 5: Registration Request

1. Add → Sampler → HTTP Request
2. Name: `1. Register Citizen`
3. Configuration:
   - Method: `POST`
   - Path: `/api/v1/citizens/register`
   - Body Data:
```json
{
  "firstName": "${firstName}",
  "lastName": "${lastName}",
  "email": "${email}",
  "password": "${password}"
}
```

4. Add → Post Processors → Response Assertion
   - Field to Test: Response Code
   - Pattern Matching Rules: Equals
   - Patterns to Test: `200`

### الخطوة 6: Get OTP from Database (JDBC Request)

1. Add → Sampler → JDBC Request
2. Name: `2. Get OTP from DB`
3. Configuration:
   - Variable Name: `postgresDB` (نفس اسم JDBC Connection Configuration)
   - SQL Query:
```sql
SELECT otp_code FROM otp_verifications WHERE email = '${email}' ORDER BY created_at DESC LIMIT 1
```

4. Add → Post Processors → JDBC PostProcessor (أو Result Set Variable Name)
   - Variable names: `otpCode`
   - Result variable: `otpResult`
   - Query type: `Select Statement`

**بديل:** استخدام JSON Extractor إذا كان JDBC Request يُرجع JSON:
- Variable name: `otpCode`
- JSON Path: `$[0].otp_code` (أو حسب الصيغة)

**ملاحظة مهمة:** JDBC Request قد يحتاج إعداد إضافي. خيار أبسط:

#### خيار بديل: استخدام JDBC Request مع Result Variable

في JDBC Request:
- Query type: `Select Statement`
- SQL Query: `SELECT otp_code FROM otp_verifications WHERE email = '${email}' ORDER BY created_at DESC LIMIT 1`
- Result variable name: `otpResult`

ثم استخدام BeanShell PostProcessor أو JSR223 PostProcessor لاستخراج القيمة:

```groovy
import groovy.json.JsonOutput

// JDBC Result في otpResult_1, otpResult_# etc
String otp = vars.get("otpResult_1") ?: vars.get("otpResult_#1")
if (otp) {
    vars.put("otpCode", otp.trim())
    log.info("Extracted OTP: " + otp)
} else {
    log.error("Failed to extract OTP from database")
}
```

### الخطوة 7: Verify OTP Request

1. Add → Sampler → HTTP Request
2. Name: `3. Verify OTP`
3. Configuration:
   - Method: `POST`
   - Path: `/api/v1/citizens/verify-otp`
   - Body Data:
```json
{
  "email": "${email}",
  "otpCode": "${otpCode}"
}
```

4. Add → Post Processors → Response Assertion
   - Response Code: `200`

### الخطوة 8: (اختياري) Login Request

1. Add → Sampler → HTTP Request
2. Name: `4. Login After Verification`
3. Configuration:
   - Method: `POST`
   - Path: `/api/v1/citizens/login`
   - Body Data:
```json
{
  "email": "${email}",
  "password": "${password}"
}
```

4. Add → Post Processors → JSON Extractor
   - Variable name: `jwtToken`
   - JSON Path: `$.token`

---

## البديل الأبسط: استخدام Timer + JDBC Query

إذا كان JDBC Request معقد، يمكن استخدام نهج أبسط:

### استخدام BeanShell/JSR223 PostProcessor بعد Registration

1. بعد Registration Request:
   - Add → Post Processors → JSR223 PostProcessor
   - Language: `groovy`
   - Script:
```groovy
import groovy.sql.Sql
import java.sql.DriverManager

// Database connection
def url = "jdbc:postgresql://localhost:5432/mithaq"
def user = "postgres"
def password = "password"
def driver = "org.postgresql.Driver"

def sql = Sql.newInstance(url, user, password, driver)

try {
    def email = vars.get("email")
    def result = sql.firstRow("SELECT otp_code FROM otp_verifications WHERE email = ? ORDER BY created_at DESC LIMIT 1", [email])
    
    if (result) {
        vars.put("otpCode", result.otp_code)
        log.info("OTP extracted: " + result.otp_code + " for email: " + email)
    } else {
        log.error("No OTP found for email: " + email)
    }
} catch (Exception e) {
    log.error("Error extracting OTP: " + e.getMessage())
} finally {
    sql.close()
}
```

---

## الحل الأبسط: فقط اختبار Registration (بدون Verify OTP)

إذا كان الهدف فقط اختبار **Registration endpoint** تحت حمولة كبيرة:

### Test Plan مبسط:

```
Thread Group (100 users, 60s ramp-up)
├── HTTP Request Defaults
├── CSV Data Set Config
├── Register Citizen Request
└── Listeners
```

**المزايا:**
- بسيط جداً
- لا يحتاج JDBC
- يختبر Registration فقط

**العيوب:**
- لا يختبر Verify OTP flow
- لا يختبر العملية الكاملة

---

## إعداد CSV Data Set Config لعدد كبير

لإنشاء ملف CSV بـ 1000 مستخدم:

### استخدام Script (Python مثال):

```python
import csv

with open('registration-users.csv', 'w', newline='', encoding='utf-8') as f:
    writer = csv.writer(f)
    writer.writerow(['firstName', 'lastName', 'email', 'password'])
    
    first_names = ['أحمد', 'محمد', 'علي', 'حسن', 'خالد', 'فاطمة', 'سارة', 'مريم']
    last_names = ['محمد', 'علي', 'حسن', 'أحمد', 'خالد', 'إبراهيم']
    
    for i in range(1, 1001):
        first = first_names[i % len(first_names)]
        last = last_names[i % len(last_names)]
        email = f'user{i}@test.com'
        password = 'Password123!'
        writer.writerow([first, last, email, password])
```

أو استخدام Excel/Google Sheets مع الصيغة:
- Email: `=CONCATENATE("user",ROW(),"@test.com")`

---

## Listeners الموصى بها

1. **View Results Tree** (للاختبار الأولي فقط - تعطيله في الاختبارات الكبيرة)
2. **Summary Report** (للنتائج الإجمالية)
3. **Aggregate Report** (للإحصائيات التفصيلية)
4. **Response Times Over Time** (لرسم بياني لوقت الاستجابة)
5. **Errors Report** (لرؤية الأخطاء فقط)

---

## معايير النجاح

### Registration Endpoint:
- **Response Time (95th percentile):** < 2 seconds
- **Success Rate:** > 95%
- **Throughput:** > 50 requests/second

### Verify OTP Endpoint:
- **Response Time (95th percentile):** < 500ms
- **Success Rate:** > 98%

---

## نصائح مهمة

### ✅ يجب فعله:
1. **استخدم Ramp-up period** - لا تبدأ بكل المستخدمين دفعة واحدة
2. **عطّل View Results Tree** في الاختبارات الكبيرة
3. **استخدم Non-GUI Mode** للأداء الأفضل:
   ```bash
   jmeter -n -t registration-test.jmx -l results.jtl -e -o html-report
   ```
4. **راقب قاعدة البيانات** - تأكد من عدم وجود Deadlocks
5. **استخدم Email فريدة** - استخدم `${__threadNum}` أو timestamp

### ❌ يجب تجنبه:
1. **لا تستخدم نفس Email مرتين** في نفس الوقت
2. **لا تبدأ بحمل كبير مباشرة** - استخدم Ramp-up
3. **لا تستخدم View Results Tree** في الاختبارات الكبيرة
4. **لا تنسَ تنظيف البيانات** بعد الاختبار (حذف المستخدمين المختبرين)

---

## خطوات التشغيل

### 1. إعداد قاعدة البيانات
- تأكد من أن PostgreSQL يعمل
- تأكد من أن قاعدة البيانات `mithaq` موجودة

### 2. إعداد JMeter
- تثبيت PostgreSQL JDBC Driver
- إنشاء Test Plan

### 3. اختبار أولي (1 مستخدم)
- Number of Threads: `1`
- Loop Count: `1`
- تشغيل ومراقبة النتائج

### 4. اختبار صغير (10 مستخدمين)
- Number of Threads: `10`
- Ramp-up: `10`
- Loop Count: `1`

### 5. اختبار متوسط (50 مستخدم)
- Number of Threads: `50`
- Ramp-up: `30`
- Loop Count: `1`

### 6. اختبار كبير (100+ مستخدم)
- Number of Threads: `100`
- Ramp-up: `60`
- Loop Count: `1`

---

## حل المشاكل الشائعة

### المشكلة: JDBC Request لا يعمل
**الحل:** 
- تأكد من تثبيت PostgreSQL JDBC Driver في `lib/`
- أعد تشغيل JMeter
- تحقق من JDBC URL وCredentials

### المشكلة: OTP not found
**الحل:**
- أضف Timer صغير (100-500ms) بعد Registration
- تأكد من أن Email نفسها المستخدمة في Registration

### المشكلة: Email already exists
**الحل:**
- استخدم Email فريدة: `user${__threadNum}${__time(,)}@test.com`
- أو نظف قاعدة البيانات قبل الاختبار

### المشكلة: الاختبار بطيء جداً
**الحل:**
- عطّل View Results Tree
- استخدم Non-GUI Mode
- قلل عدد المستخدمين أو زد Ramp-up period

---

## مثال Test Plan Structure

```
Test Plan: Registration Load Test
└── JDBC Connection Configuration (postgresDB)
└── Thread Group (100 users, 60s ramp-up, 1 loop)
    ├── HTTP Request Defaults (localhost:13000)
    ├── CSV Data Set Config (registration-users.csv)
    ├── 1. Register Citizen
    │   └── Response Assertion (200)
    ├── Timer (Fixed Delay: 200ms)
    ├── 2. Get OTP from DB (JDBC Request)
    │   └── JSR223 PostProcessor (استخراج OTP)
    ├── 3. Verify OTP
    │   └── Response Assertion (200)
    ├── 4. Login After Verification (اختياري)
    │   └── JSON Extractor (jwtToken)
    └── Listeners
        ├── Summary Report
        ├── Aggregate Report
        └── Errors Report
```

---

## الخلاصة

لاختبار Registration + OTP لعدد كبير من المستخدمين:

1. ✅ استخدم **CSV Data Set Config** لبيانات المستخدمين
2. ✅ استخدم **JDBC Request** لجلب OTP من قاعدة البيانات
3. ✅ استخدم **Ramp-up period** للزيادة التدريجية
4. ✅ استخدم **Non-GUI Mode** للاختبارات الكبيرة
5. ✅ راقب **Response Time** و **Success Rate**

**ابدأ باختبار صغير (10 مستخدمين) ثم زد العدد تدريجياً! 🚀**

