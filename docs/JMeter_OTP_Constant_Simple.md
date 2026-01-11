# استخدام OTP ثابت للاختبار في JMeter

## السؤال: كيف أستخدم OTP ثابت للاختبار بدون تعديل منطق التطبيق؟

**الجواب:** يمكنك وضع OTP ثابت في JMeter فقط (للاستخدام في Verify OTP Request).

---

## ⚠️ ملاحظة مهمة

**OTP ثابت في JMeter لن يعمل** ما لم تقم بتعديل قاعدة البيانات يدوياً لحفظ نفس OTP، أو تستخدم OTP معروف مسبقاً.

**الخيار الأفضل:** استخدم JDBC Request لجلب OTP الفعلي من قاعدة البيانات (راجع `JMeter_Registration_Load_Test_Guide.md`).

---

## الطريقة 1: User Defined Variables (الأبسط)

### الخطوات:

1. **في Test Plan:**
   - Right Click على "Test Plan"
   - Add → Config Element → User Defined Variables
   - Add Variable:
     - **Name:** `otpCode`
     - **Value:** `123456`

2. **في Verify OTP Request:**
   - Body Data:
```json
{
  "email": "${email}",
  "otpCode": "${otpCode}"
}
```

---

## الطريقة 2: CSV Data Set Config (الأفضل)

### الخطوة 1: تعديل ملف CSV

أضف عمود `otpCode`:

```csv
firstName,lastName,email,password,otpCode
أحمد,محمد,user1@test.com,Password123!,123456
علي,حسن,user2@test.com,Password123!,123456
فاطمة,علي,user3@test.com,Password123!,123456
```

### الخطوة 2: في JMeter

1. **CSV Data Set Config:**
   - Variable names: `firstName,lastName,email,password,otpCode`

2. **Verify OTP Request Body:**
```json
{
  "email": "${email}",
  "otpCode": "${otpCode}"
}
```

---

## مثال كامل: Request Body Data

### Registration Request:
```json
{
  "firstName": "${firstName}",
  "lastName": "${lastName}",
  "email": "${email}",
  "password": "${password}"
}
```

### Verify OTP Request:
```json
{
  "email": "${email}",
  "otpCode": "${otpCode}"
}
```

---

## ⚠️ تحذير: OTP ثابت لن يعمل مع النظام الحالي

النظام يولد OTP **عشوائياً** في كل مرة. OTP ثابت في JMeter سيفشل في Verify OTP لأن OTP المحفوظ في قاعدة البيانات مختلف.

### الحلول:

1. **استخدام JDBC Request** (الأفضل - راجع `JMeter_Registration_Load_Test_Guide.md`)
2. **تعديل قاعدة البيانات يدوياً** بعد كل Registration (غير عملي)
3. **قراءة OTP من Logs** (للتطوير فقط)

---

## الحل الموصى به: JDBC Request

راجع `JMeter_Registration_Load_Test_Guide.md` للطريقة الصحيحة باستخدام JDBC Request لجلب OTP الفعلي من قاعدة البيانات.

