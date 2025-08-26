package com.assignment.phoneinventory.dto;

public class StepStatus {
    public String stepName;
    public String status;
    public long readCount;
    public long writeCount;
    public long readSkipCount;
    public long writeSkipCount;
    public long processSkipCount;

    public StepStatus() {}
    public StepStatus(String stepName, String status,
                      long readCount, long writeCount,
                      long readSkipCount, long writeSkipCount,
                      long processSkipCount) {
        this.stepName = stepName;
        this.status = status;
        this.readCount = readCount;
        this.writeCount = writeCount;
        this.readSkipCount = readSkipCount;
        this.writeSkipCount = writeSkipCount;
        this.processSkipCount = processSkipCount;
    }
}
