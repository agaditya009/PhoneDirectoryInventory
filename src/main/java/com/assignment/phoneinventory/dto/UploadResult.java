package com.assignment.phoneinventory.dto;

public class UploadResult {
    public long processed;
    public long inserted;
    public long skippedDuplicates;
    public UploadResult(long processed, long inserted, long skippedDuplicates) {
        this.processed = processed;
        this.inserted = inserted;
        this.skippedDuplicates = skippedDuplicates;
    }
}
