package com.example.pdfformfiller.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class ReportData {
    private int totalProperties;
    private List<FieldResult> fieldResults;
    private List<String> unfilledPdfFields;
    private Instant processingTimestamp;

    public int getFilledCount() {
        return (int) fieldResults.stream().filter(r -> r.getStatus() == FieldStatus.FILLED).count();
    }

    public int getNotFoundCount() {
        return (int) fieldResults.stream().filter(r -> r.getStatus() == FieldStatus.FIELD_NOT_FOUND).count();
    }

    public int getErrorCount() {
        return (int) fieldResults.stream().filter(r -> r.getStatus() == FieldStatus.ERROR).count();
    }
}
