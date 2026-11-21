package com.teamwork.backend.common.health.dto;

/**
 * @param status trạng thái của backend; luôn là {@code "UP"} khi backend trả lời được
 */
public record HealthResponse(String status) {

}
