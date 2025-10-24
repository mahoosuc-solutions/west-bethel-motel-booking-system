package com.westbethel.motel_booking.common.model;

import java.util.Arrays;
import java.util.Currency;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enum representing supported currencies in the booking system.
 * This provides an allowlist approach to prevent currency injection attacks.
 */
public enum SupportedCurrency {
    USD("USD", "US Dollar"),
    EUR("EUR", "Euro"),
    GBP("GBP", "British Pound"),
    CAD("CAD", "Canadian Dollar");

    private final String code;
    private final String displayName;

    SupportedCurrency(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Currency toCurrency() {
        return Currency.getInstance(code);
    }

    /**
     * Get all supported currency codes.
     */
    public static Set<String> getSupportedCodes() {
        return Arrays.stream(values())
                .map(SupportedCurrency::getCode)
                .collect(Collectors.toSet());
    }

    /**
     * Check if a currency code is supported.
     */
    public static boolean isSupported(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return false;
        }
        return getSupportedCodes().contains(currencyCode.toUpperCase());
    }

    /**
     * Get SupportedCurrency from currency code.
     */
    public static SupportedCurrency fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Currency code cannot be null or blank");
        }
        return Arrays.stream(values())
                .filter(c -> c.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unsupported currency code: " + code + ". Supported currencies: " + getSupportedCodes()));
    }
}
