package com.assignment.phoneinventory.dto;

import com.assignment.phoneinventory.domain.TelephoneNumber;

public class SearchRequest {
    public String countryCode;
    public String areaCode;
    public String prefix;
    public String contains;
    public TelephoneNumber.Status status;
    public int page = 0;
    public int size = 20;
    public String digitsPrefix;
}
