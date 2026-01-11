/**
 * JSR223 PostProcessor Script for JMeter
 * Extracts OTP code from JDBC Result
 * 
 * Place this script in a JSR223 PostProcessor after JDBC Request
 * Language: groovy
 */

import groovy.sql.Sql
import java.sql.DriverManager

// Database connection details
def url = "jdbc:postgresql://localhost:5432/mithaq"
def user = "postgres"
def password = "password"
def driver = "org.postgresql.Driver"

// Get email from JMeter variables
def email = vars.get("email")

if (!email) {
    log.error("Email variable not found!")
    prev.setSuccessful(false)
    prev.setResponseMessage("Email variable not found")
    return
}

def sql = null
try {
    // Create database connection
    sql = Sql.newInstance(url, user, password, driver)
    
    // Query OTP from database
    def query = "SELECT otp_code FROM otp_verifications WHERE email = ? ORDER BY created_at DESC LIMIT 1"
    def result = sql.firstRow(query, [email])
    
    if (result && result.otp_code) {
        def otpCode = result.otp_code.toString().trim()
        vars.put("otpCode", otpCode)
        log.info("✅ OTP extracted successfully: " + otpCode + " for email: " + email)
        prev.setSuccessful(true)
    } else {
        log.error("❌ No OTP found in database for email: " + email)
        prev.setSuccessful(false)
        prev.setResponseMessage("OTP not found in database")
    }
    
} catch (Exception e) {
    log.error("❌ Error extracting OTP: " + e.getMessage())
    e.printStackTrace()
    prev.setSuccessful(false)
    prev.setResponseMessage("Database error: " + e.getMessage())
} finally {
    // Close database connection
    if (sql) {
        try {
            sql.close()
        } catch (Exception e) {
            log.warn("Error closing database connection: " + e.getMessage())
        }
    }
}

