package com.audora.inhash.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {
    private String email;
    private String username;
}