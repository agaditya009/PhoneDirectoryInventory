package com.assignment.phoneinventory.search;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.assignment.phoneinventory.domain.TelephoneNumber;

@Document(indexName = "telephone_numbers")
public class TelephoneNumberDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String number;

    @Field(type = FieldType.Keyword)
    private String countryCode;

    @Field(type = FieldType.Keyword)
    private String areaCode;

    @Field(type = FieldType.Keyword)
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
        t.setStatus(status);
        t.setAllocatedUserId(allocatedUserId);
        t.setReservedUntil(reservedUntil);
        t.setVersion(version);
        t.setNumberDigits(numberDigits);
        return t;
    }
}
