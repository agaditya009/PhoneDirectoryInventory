package com.assignment.phoneinventory.batch;

import java.time.Instant;

public class PhoneCsv {

    // Fields read from CSV
    private String number;       // e.g. "+91-8079-123456"
    private String countryCode;  // e.g. "+91"
    private String areaCode;     // e.g. "080"
    private String allocatedUserId;
    private Instant reservedUntil;

    // Computed in ItemProcessor for fast search; persisted by writer as :numberDigits
    private String numberDigits; // digits-only form of 'number' (e.g. "918079123456")

    public PhoneCsv() { }

    // ----- getters & setters (trim to be CSV-friendly) -----
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = trimOrNull(number); }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = trimOrNull(countryCode); }

    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = trimOrNull(areaCode); }

    public String getAllocatedUserId() { return allocatedUserId; }
    public void setAllocatedUserId(String allocatedUserId) { this.allocatedUserId = trimOrNull(allocatedUserId); }

    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }

    public String getNumberDigits() { return numberDigits; }
    public void setNumberDigits(String numberDigits) { this.numberDigits = trimOrNull(numberDigits); }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    @Override
    public String toString() {
        return "PhoneCsv{" +
                "number='" + number + '\'' +
                ", countryCode='" + countryCode + '\'' +
                ", areaCode='" + areaCode + '\'' +
                ", allocatedUserId='" + allocatedUserId + '\'' +
                ", reservedUntil='" + reservedUntil + '\'' +
                ", numberDigits='" + numberDigits + '\'' +
                '}';
    }
}
