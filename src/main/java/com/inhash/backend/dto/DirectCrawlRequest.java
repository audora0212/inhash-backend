package com.inhash.backend.dto;

import lombok.Data;

@Data
public class DirectCrawlRequest {
    private Long studentId;
    private String lmsUsername;
    private String lmsPassword;
}

