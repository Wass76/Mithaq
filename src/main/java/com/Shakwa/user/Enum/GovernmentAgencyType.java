package com.Shakwa.user.Enum;

import com.fasterxml.jackson.annotation.JsonValue;

public enum GovernmentAgencyType {

    وزارة_الإدارة_المحلية_والبيئة("mola", "Ministry of Local Administration"),
    وزارة_المالية("mof", "Ministry of Finance"),
    وزارة_الدفاع("mod", "Ministry of Defense"),
    وزارة_الاقتصاد_والصناعة("moei", "Ministry of Economy and Industry"),
    وزارة_التعليم_العالي("mohe", "Ministry of Higher Education"),
    وزارة_الصحة("moh", "Ministry of Health"),
    وزارة_التربية("moe", "Ministry of Education"),
    وزارة_الطاقة("moen", "Ministry of Energy"),
    أمانة_رئاسة_مجلس_الوزراء("cabinet", "Cabinet Secretariat"),
    وزارة_الأشغال_العامة_والإسكان("mopwh", "Ministry of Public Works and Housing"),
    وزارة_الاتصالات_والتقانة("moct", "Ministry of Communications and Technology"),
    وزارة_الداخلية("moi", "Ministry of Interior"),
    وزارة_الزراعة("moag", "Ministry of Agriculture"),
    وزارة_الشؤون_الاجتماعية_والعمل("mosal", "Ministry of Social Affairs and Labor"),
    وزارة_الثقافة("moc", "Ministry of Culture"),
    وزارة_النقل("mot", "Ministry of Transport"),
    وزارة_العدل("moj", "Ministry of Justice"),
    وزارة_السياحة("motour", "Ministry of Tourism"),
    وزارة_الإعلام("moinfo", "Ministry of Information"),
    وزارة_الأوقاف("moawqaf", "Ministry of Awqaf"),
    نقابة_المعلمين("teachers", "Teachers Syndicate"),
    الاتحاد_الرياضي_العام("sports", "General Sports Union"),
    الاتحاد_العام_للفلاحين("farmers", "General Farmers Union"),
    مجلس_الدولة("statecnl", "State Council"),
    وزارة_التنمية_الإدارية("moad", "Ministry of Administrative Development"),
    وزارة_الخارجية_والمغتربين("mofa", "Ministry of Foreign Affairs"),
    وزارة_الطوارئ_والكوارث("emergency", "Ministry of Emergency"),
    الهيئة_العامة_للمنافذ_البرية_والبحرية("ports", "Ports Authority"),
    مصرف_سوريا_المركزي("cbs", "Central Bank of Syria");

    private final String emailCode;
    private final String englishName;

    GovernmentAgencyType(String emailCode, String englishName) {
        this.emailCode = emailCode;
        this.englishName = englishName;
    }

    @JsonValue
    public String getLabel() {
        return this.name().replace("_", " ");
    }

    /**
     * Returns the short code used in email addresses
     * Example: mof, moe, moh
     */
    public String getEmailCode() {
        return emailCode;
    }

    /**
     * Returns the English name of the agency
     */
    public String getEnglishName() {
        return englishName;
    }
}
