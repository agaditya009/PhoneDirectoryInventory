package com.assignment.phoneinventory.search;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import java.time.Instant;

@Document(indexName = "telephone_numbers")
public class TelephoneDocument {
    @Id private String number;
    private String countryCode;
    private String areaCode;
    private String status;
    private String numberDigits;
    private String allocatedUserId;
    @Field(type = FieldType.Date, format = DateFormat.epoch_millis)
    private Instant reservedUntil;

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getAreaCode() { return areaCode; }
    public void setAreaCode(String areaCode) { this.areaCode = areaCode; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNumberDigits() { return numberDigits; }
    public void setNumberDigits(String numberDigits) { this.numberDigits = numberDigits; }
    public String getAllocatedUserId() { return allocatedUserId; }
    public void setAllocatedUserId(String allocatedUserId) { this.allocatedUserId = allocatedUserId; }
    public Instant getReservedUntil() { return reservedUntil; }
    public void setReservedUntil(Instant reservedUntil) { this.reservedUntil = reservedUntil; }
}

