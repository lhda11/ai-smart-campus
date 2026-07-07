package com.campus.core.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank
    private String keyword;
}
