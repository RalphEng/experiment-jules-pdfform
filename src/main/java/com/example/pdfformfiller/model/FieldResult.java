package com.example.pdfformfiller.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class FieldResult {
    private String propertyKey;
    private String propertyValue;
    private String pdfFieldName;
    private FieldStatus status;
    private String fieldType;
    private String errorMessage;
}
