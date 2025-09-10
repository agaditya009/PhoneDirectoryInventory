package com.assignment.phoneinventory.constants;


import static com.assignment.phoneinventory.dao.TelephoneCols.*;

/** Central SQL for telephone_numbers. Keep DAOs free of raw SQL strings. */
public final class TelephoneConstants {
    private TelephoneConstants() {}

    public static final String TABLE = "telephone_numbers";

    public static final String SELECT_COLUMNS =
            String.join(",",
                ID, NUMBER, COUNTRY_CODE, AREA_CODE, STATUS,
                ALLOCATED_USER_ID, RESERVED_UNTIL, VERSION
            );

    // Base select for searches (WHERE 1=1 tailors dynamically)
    public static final String SELECT_BASE =
            "SELECT " + SELECT_COLUMNS + " FROM " + TABLE + " WHERE 1=1";

    // Find by id
    public static final String SELECT_BY_ID =
            "SELECT " + SELECT_COLUMNS + " FROM " + TABLE + " WHERE " + ID + " = ?";

    // Find by phone number
    public static final String SELECT_BY_NUMBER =
            "SELECT " + SELECT_COLUMNS + " FROM " + TABLE + " WHERE " + NUMBER + " = ?";

    // Pagination tail
    public static final String SEARCH_ORDER_LIMIT_OFFSET =
            " ORDER BY " + ID + " ASC LIMIT ? OFFSET ?";

    // Count base
    public static final String COUNT_BASE =
            "SELECT COUNT(*) FROM " + TABLE + " WHERE 1=1";

    // Optimistic update with version check
    public static final String UPDATE_WITH_VERSION =
            "UPDATE " + TABLE + " SET " +
                    STATUS + "=?," +
                    ALLOCATED_USER_ID + "=?," +
                    RESERVED_UNTIL + "=?," +
                    VERSION + "=" + VERSION + "+1 " +
            "WHERE " + ID + "=? AND " + VERSION + "=?";

    // Exists by number
    public static final String EXISTS_BY_NUMBER =
            "SELECT COUNT(*) FROM " + TABLE + " WHERE " + NUMBER + " = ?";

    // Plain insert
    public static final String INSERT_ROW =
            "INSERT INTO " + TABLE + "(" +
                    String.join(",", NUMBER, COUNTRY_CODE, AREA_CODE, STATUS,
                                      ALLOCATED_USER_ID, RESERVED_UNTIL, VERSION) +
            ") VALUES (?,?,?,?,?,?,?)";
}
