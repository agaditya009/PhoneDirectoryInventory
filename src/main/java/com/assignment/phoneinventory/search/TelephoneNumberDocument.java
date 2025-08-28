package com.assignment.phoneinventory.search;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import com.assignment.phoneinventory.domain.TelephoneNumber;

@Document(indexName = "telephone_numbers")
public class TelephoneNumberDocument {

    @Id
    private Long id;
    private String number;
    private String countryCode;
    private String areaCode;
    private String prefix;
    private TelephoneNumber.Status status;
    private String allocatedUserId;
    private Instant reservedUntil;
    private long version;
    private String numberDigits;

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

    public TelephoneNumber.Status getStatus() { return status; }
    public void setStatus(TelephoneNumber.Status status) { this.status = status; }

    public String getAllocatedUserId() { return allocatedUserId; }
    public void setAllocatedUserId(String allocatedUserId) { this.allocatedUserId = allocatedUserId; }

    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }

    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }

    public String getNumberDigits() { return numberDigits; }
    public void setNumberDigits(String numberDigits) { this.numberDigits = numberDigits; }

    public static TelephoneNumberDocument from(TelephoneNumber t) {
        TelephoneNumberDocument d = new TelephoneNumberDocument();
        d.setId(t.getId());
        d.setNumber(t.getNumber());
        d.setCountryCode(t.getCountryCode());
        d.setAreaCode(t.getAreaCode());
        d.setPrefix(t.getPrefix());
        d.setStatus(t.getStatus());
        d.setAllocatedUserId(t.getAllocatedUserId());
        d.setReservedUntil(t.getReservedUntil());
        d.setVersion(t.getVersion());
        d.setNumberDigits(t.getNumberDigits());
        return d;
    }

    public TelephoneNumber toDomain() {
        TelephoneNumber t = new TelephoneNumber();
        t.setId(id);
        t.setNumber(number);
        t.setCountryCode(countryCode);
        t.setAreaCode(areaCode);
        t.setPrefix(prefix);
        t.setStatus(status);
        t.setAllocatedUserId(allocatedUserId);
        t.setReservedUntil(reservedUntil);
        t.setVersion(version);
        t.setNumberDigits(numberDigits);
        return t;
    }
}
