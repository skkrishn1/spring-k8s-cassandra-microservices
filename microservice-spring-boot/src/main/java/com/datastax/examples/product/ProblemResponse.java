package com.datastax.examples.product;

import java.time.Instant;

public class ProblemResponse {
    private String type;
    private String title;
    private int status;
    private String detail;
    private String traceId;
    private Instant timestamp;

    public ProblemResponse() {
    }

    public ProblemResponse(String type, String title, int status, String detail, String traceId) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.traceId = traceId;
        this.timestamp = Instant.now();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
