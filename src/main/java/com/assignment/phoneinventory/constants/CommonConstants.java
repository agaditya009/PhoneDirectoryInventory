package com.assignment.phoneinventory.constants;

public final class CommonConstants {
    private CommonConstants() {}

    // Job parameter keys
    public static final String FILE_PATH = "file.path";
    public static final String JOB_ID = "jobId";
    public static final String TIME = "time";

    // Error messages
    public static final String CONCURRENT_UPDATE_DETECTED = "Concurrent update detected";
    public static final String RESERVATION_EXPIRED = "Reservation expired";
    public static final String ONLY_AVAILABLE_CAN_BE_RESERVED = "Only AVAILABLE numbers can be reserved";
    public static final String ONLY_RESERVED_CAN_BE_ALLOCATED = "Only RESERVED numbers can be allocated";
    public static final String NUMBER_RESERVED_FOR_DIFFERENT_USER = "Number reserved for a different user";
    public static final String ONLY_ALLOCATED_CAN_BE_ACTIVATED = "Only ALLOCATED numbers can be activated";
    public static final String NUMBER_ALLOCATED_TO_DIFFERENT_USER = "Number allocated to a different user";
    public static final String ONLY_ACTIVATED_CAN_BE_DEACTIVATED = "Only ACTIVATED numbers can be deactivated";

    // Audit notes
    public static final String RESERVED_FOR_TEMPLATE = "Reserved for %d min";
    public static final String ALLOCATED = "Allocated";
    public static final String ACTIVATED = "Activated";
    public static final String DEACTIVATED = "Deactivated";

    // CSV import messages
    public static final String EXPECTED_COLUMNS = "Expected columns: number,countryCode,areaCode";
    public static final String MISSING_REQUIRED_VALUE_AT_LINE = "Missing required value at line %d";
    public static final String ERROR_READING_CSV_FILE = "Error reading CSV file: %s";
    public static final String MISSING_REQUIRED_COLUMN = "Missing required column";

    // Generic messages
    public static final String NUMBER_NOT_FOUND = "Number %s not found";
}
