package com.teamwork.backend.common.error;

/**
 * Một phần tử của trường {@code errors} khi {@code code} là {@code VALIDATION_ERROR}.
 * {@code field} là {@code null} khi lỗi không gắn với một trường cụ thể.
 */
public record FieldErrorDetail(String field, String message) {
}
