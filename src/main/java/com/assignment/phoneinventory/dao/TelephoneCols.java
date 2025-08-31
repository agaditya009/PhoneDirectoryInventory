package com.assignment.phoneinventory.dao;

/** Column name constants for telephone_numbers (and related) tables. */
public final class TelephoneCols {
    private TelephoneCols() {}

    public static final String ID               = "id";
    public static final String NUMBER           = "number";
    public static final String COUNTRY_CODE     = "country_code";
    public static final String AREA_CODE        = "area_code";
    public static final String STATUS           = "status";
    public static final String ALLOCATED_USER_ID= "allocated_user_id";
    public static final String RESERVED_UNTIL   = "reserved_until";
    public static final String VERSION          = "version";
    public static final String NUMBER_DIGITS    = "number_digits";
}
