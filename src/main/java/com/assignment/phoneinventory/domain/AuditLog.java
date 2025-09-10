package com.assignment.phoneinventory.domain;

import java.time.Instant;

public class AuditLog {
    private Long id;
    private Long numberId;
    private String fromState;
    private String toState;
    private String userId;
    private Instant timestamp;
    private String note;

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getNumberId() { return numberId; }
    public void setNumberId(Long numberId) { this.numberId = numberId; }
    public String getFromState() { return fromState; }
    public void setFromState(String fromState) { this.fromState = fromState; }
    public String getToState() { return toState; }
    public void setToState(String toState) { this.toState = toState; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
