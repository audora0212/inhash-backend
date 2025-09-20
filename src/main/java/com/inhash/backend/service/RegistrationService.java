package com.inhash.backend.service;

import com.inhash.backend.domain.Student;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {

    private final AuthService authService;
    private final LmsAccountService lmsAccountService;

    public RegistrationService(AuthService authService, LmsAccountService lmsAccountService) {
        this.authService = authService;
        this.lmsAccountService = lmsAccountService;
    }

    @Transactional
    public Student registerWithLms(String email, String name, String rawPassword,
                                   String lmsUsername, String lmsPassword) {
        Student s = authService.register(email, name, rawPassword);
        lmsAccountService.registerLmsAccount(s.getId(), lmsUsername, lmsPassword);
        return s;
    }
}


