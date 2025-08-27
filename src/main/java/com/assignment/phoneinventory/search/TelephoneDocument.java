package com.assignment.phoneinventory.search;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

@Document(indexName = "telephones")
public class TelephoneDocument {

    @Id
    private Long id;
    private String number;
    private String countryCode;
    private String areaCode;
    private String prefix;
    private String status;
    private String allocatedUserId;
    private Instant reservedUntil;
    private long version;
    private String numberDigits;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getAllocatedUserId() { return allocatedUserId; }
    public void setAllocatedUserId(String allocatedUserId) { this.allocatedUserId = allocatedUserId; }
    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
    public String getNumberDigits() { return numberDigits; }
    public void setNumberDigits(String numberDigits) { this.numberDigits = numberDigits; }
}
