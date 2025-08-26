package com.assignment.phoneinventory.es;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

import com.assignment.phoneinventory.domain.TelephoneNumber;

@Document(indexName = "telephone_numbers")
@Setting(settingPath = "/elasticsearch/telephone-settings.json")
public class TelephoneNumberDocument {

    @Id
    private String id; // prefer DB id; fallback to number

    @Field(type = FieldType.Text, analyzer = "standard")
    private String number;

    @Field(type = FieldType.Keyword)
    private String countryCode;

    @Field(type = FieldType.Keyword)
    private String areaCode;

    @Field(type = FieldType.Keyword)
    private String prefix;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(name = "numberDigits", type = FieldType.Text, analyzer = "digits_analyzer")
    private String numberDigits;

    public static TelephoneNumberDocument from(TelephoneNumber t) {
        TelephoneNumberDocument d = new TelephoneNumberDocument();
        d.id = t.getId() != null ? String.valueOf(t.getId()) : t.getNumber();
        d.number = t.getNumber();
        d.countryCode = t.getCountryCode();
        d.areaCode = t.getAreaCode();
        d.prefix = t.getPrefix();
        d.status = t.getStatus() == null ? null : t.getStatus().name();
        d.numberDigits = t.getNumberDigits();
        return d;
    }

    public TelephoneNumber toDomain() {
        TelephoneNumber t = new TelephoneNumber();
        if (id != null) {
            try { t.setId(Long.parseLong(id)); } catch (NumberFormatException ignored) {}
        }
        t.setNumber(number);
        t.setCountryCode(countryCode);
        t.setAreaCode(areaCode);
        t.setPrefix(prefix);
        if (status != null) {
            t.setStatus(TelephoneNumber.Status.valueOf(status));
        }
        t.setNumberDigits(numberDigits);
        return t;
    }

    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
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
    public String getNumberDigits() { return numberDigits; }
    public void setNumberDigits(String numberDigits) { this.numberDigits = numberDigits; }
}
