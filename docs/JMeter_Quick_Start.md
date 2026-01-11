# دليل البدء السريع لـ JMeter

## الخطوات الأولى (خطوة بخطوة)

### 1. تثبيت JMeter
```bash
# تحميل من: https://jmeter.apache.org/download_jmeter.cgi
# استخراج الملف المضغوط
# تشغيل: bin/jmeter.bat (Windows) أو bin/jmeter.sh (Linux/Mac)
```

### 2. إنشاء Test Plan بسيط

#### الخطوة 2.1: Thread Group
1. Click على "Test Plan" (أقصى اليسار)
2. Right Click → Add → Threads (Users) → Thread Group
3. Set:
   - Number of Threads: `10` (عدد المستخدمين)
   - Ramp-up period: `10` (ثواني لزيادة العدد تدريجياً)
   - Loop Count: `5` (عدد المرات التي يعيد كل مستخدم الطلب)

#### الخطوة 2.2: HTTP Request Defaults
1. Right Click على "Thread Group"
2. Add → Config Element → HTTP Request Defaults
3. Set:
   - Server Name: `localhost`
   - Port Number: `13000`
   - Protocol: `http`

#### الخطوة 2.3: HTTP Cookie Manager
1. Right Click على "Thread Group"
2. Add → Config Element → HTTP Cookie Manager
3. (للحفاظ على Cookies/ Session)

#### الخطوة 2.4: Login Request

**خيار 1: Admin Login (الأسهل - مستخدمين جاهزين)** ⭐
1. Right Click على "Thread Group"
2. Add → Sampler → HTTP Request
3. Name: `Login - Admin`
4. Set:
   - Method: `POST`
   - Path: `/api/v1/admin/login`
   - Body Data (في تبويب "Body Data"):
```json
{
  "email": "admin@mithaq.com",
  "password": "Password!1"
}
```

**ملاحظة:** يمكن استخدام `super.admin@mithaq.com` أيضاً (نفس كلمة المرور)

**خيار 2: Citizen Login (يتطلب مستخدم verified)**
1. Right Click على "Thread Group"
2. Add → Sampler → HTTP Request
3. Name: `Login - Citizen`
4. Set:
   - Method: `POST`
   - Path: `/api/v1/citizens/login`
   - Body Data (في تبويب "Body Data"):
```json
{
  "email": "test@example.com",
  "password": "password123"
}
```

**ملاحظة:** Citizen يجب أن يكون verified (تم التحقق من OTP) أولاً.
راجع `JMeter_Login_Testing_Guide.md` للتفاصيل الكاملة.

#### الخطوة 2.5: JSON Extractor (لاستخراج Token)
1. Right Click على "Login - Citizen"
2. Add → Post Processors → JSON Extractor
3. Set:
   - Variable names: `jwtToken`
   - JSON Path Expressions: `$.token` (أو حسب استجابة API)
   - Match No.: `1`

#### الخطوة 2.6: HTTP Header Manager
1. Right Click على "Thread Group"
2. Add → Config Element → HTTP Header Manager
3. Add Header:
   - Name: `Authorization`
   - Value: `Bearer ${jwtToken}`

#### الخطوة 2.7: Test Request (جلب الشكاوى)
1. Right Click على "Thread Group"
2. Add → Sampler → HTTP Request
3. Name: `Get Complaints`
4. Set:
   - Method: `GET`
   - Path: `/api/v1/complaints?page=0&size=10`

#### الخطوة 2.8: Listeners (لرؤية النتائج)
1. Right Click على "Thread Group"
2. Add → Listener → View Results Tree
3. Add → Listener → Summary Report
4. Add → Listener → Aggregate Report

### 3. حفظ وتشغيل
1. File → Save Test Plan (اسم الملف: `shakwa-test-plan.jmx`)
2. Run → Start (أو Ctrl+R)
3. مشاهدة النتائج في Listeners

---

## مثال JSON للـ Login Request Body

### Admin Login (جاهز للاستخدام مباشرة): ⭐
```json
{
  "email": "admin@mithaq.com",
  "password": "Password!1"
}
```
أو:
```json
{
  "email": "super.admin@mithaq.com",
  "password": "Password!1"
}
```

### Citizen Login (يحتاج مستخدم verified):
```json
{
  "email": "citizen@example.com",
  "password": "password123"
}
```

### Employee Login:
```json
{
  "email": "employee@example.com",
  "password": "password123"
}
```

**💡 نصيحة:** ابدأ بـ Admin Login - إنه الأسهل ولا يحتاج Registration!

---

## نصائح سريعة

1. **للاختبارات الكبيرة:** استخدم Non-GUI Mode:
   ```bash
   jmeter -n -t shakwa-test-plan.jmx -l results.jtl -e -o html-report
   ```

2. **إضافة بيانات متعددة:** استخدم CSV Data Set Config:
   - Add → Config Element → CSV Data Set Config
   - Filename: `path/to/test-users.csv`
   - Variable names: `email,password`

3. **تعطيل View Results Tree في الاختبارات الكبيرة:**
   - Right Click على View Results Tree → Disable

4. **مراقبة الأداء:**
   - استخدم Summary Report و Aggregate Report
   - انتبه لـ Response Time (95th percentile)

---

## الأخطاء الشائعة وحلولها

### الخطأ: "Connection refused"
- **السبب:** التطبيق غير شغال على المنفذ 13000
- **الحل:** تأكد من تشغيل Spring Boot application

### الخطأ: "401 Unauthorized"
- **السبب:** Token غير صالح أو منتهي الصلاحية
- **الحل:** تأكد من JSON Extractor يستخرج Token بشكل صحيح

### الخطأ: JMeter بطيء جداً
- **السبب:** View Results Tree مفعل في اختبار كبير
- **الحل:** Disable View Results Tree أو استخدم Non-GUI Mode

---

## الخطوة التالية

بعد إتقان الأساسيات، راجع الملفات التالية:
- 📄 `JMeter_Testing_Guide.md` - الدليل الكامل
- 📄 `JMeter_Login_Testing_Guide.md` - دليل شامل لاختبار Login (بدون Registration)

---

## 💡 إجابة سريعة: هل يمكن اختبار Login بدون Registration؟

**نعم! ✅** 

- استخدم **Admin users الجاهزين:**
  - Email: `admin@mithaq.com` أو `super.admin@mithaq.com`
  - Password: `Password!1`
  - Endpoint: `POST /api/v1/admin/login`

راجع `JMeter_Login_Testing_Guide.md` للتفاصيل الكاملة!

