package com.assignment.phoneinventory.domain;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TelephoneNumber {

    public enum Status {
        AVAILABLE, RESERVED, ALLOCATED, ACTIVATED, DEACTIVATED
    }

    @JsonIgnore
    private Long id;
    private String number;
    private String countryCode;
    private String areaCode;
    private Status status;
    private String allocatedUserId;
    private Instant reservedUntil;
    @JsonIgnore
    private long version;

    /** Digits-only normalized form of the number (e.g., "+91-8079-..." -> "918079...").
     *  Used for fast search via the number_digits column. Optional for existing rows. */
    @JsonIgnore
    private String numberDigits;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }


    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getAllocatedUserId() { return allocatedUserId; }
    public void setAllocatedUserId(String allocatedUserId) { this.allocatedUserId = allocatedUserId; }

    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public String getNumberDigits() { return numberDigits; }
    public void setNumberDigits(String numberDigits) { this.numberDigits = numberDigits; }
}
