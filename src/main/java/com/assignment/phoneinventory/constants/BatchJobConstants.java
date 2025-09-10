package com.assignment.phoneinventory.constants;

/** Central SQL for batch_jobs. Keep DAOs free of raw SQL strings. */
public final class BatchJobConstants {

    private BatchJobConstants() {}

    public static final String INSERT_QUEUED =
            "INSERT INTO batch_jobs(job_id,file_name,status,created_at,updated_at,processed_records,failed_records) "
                    + "VALUES (?,?,'QUEUED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)";

    public static final String MARK_RUNNING =
            "UPDATE batch_jobs SET status='RUNNING', started_at=COALESCE(started_at,CURRENT_TIMESTAMP), "
                    + "updated_at=CURRENT_TIMESTAMP WHERE job_id=?";

    public static final String HEARTBEAT =
            "UPDATE batch_jobs SET processed_records=processed_records+?, failed_records=failed_records+?, "
                    + "updated_at=CURRENT_TIMESTAMP WHERE job_id=?";

    public static final String COMPLETE =
            "UPDATE batch_jobs SET status='COMPLETED', ended_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE job_id=?";

    public static final String FAIL =
            "UPDATE batch_jobs SET status='FAILED', ended_at=CURRENT_TIMESTAMP, error_message=?, updated_at=CURRENT_TIMESTAMP WHERE job_id=?";

    public static final String FIND_BY_JOB_ID =
            "SELECT * FROM batch_jobs WHERE job_id=?";

    public static final String UPSERT_TELEPHONE_NUMBER =
            "INSERT INTO telephone_numbers " +
                    "(number, country_code, area_code, status, version, number_digits) " +
                    "VALUES (:number, :countryCode, :areaCode, 'AVAILABLE', 0, :numberDigits) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "country_code = IF(VALUES(country_code) <> country_code, VALUES(country_code), country_code), " +
                    "area_code = IF(VALUES(area_code) <> area_code, VALUES(area_code), area_code), " +
                    "status = IF(status <> 'AVAILABLE', 'AVAILABLE', status), " +
                    "version = IF(version <> 0, 0, version), " +
                    "number_digits = IF(VALUES(number_digits) <> number_digits, VALUES(number_digits), number_digits)";
}

