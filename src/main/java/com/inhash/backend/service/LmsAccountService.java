package com.inhash.backend.service;

import com.inhash.backend.domain.LMSAccount;
import com.inhash.backend.domain.Student;
import com.inhash.backend.repository.LMSAccountRepository;
import com.inhash.backend.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LmsAccountService {

    private final LMSAccountRepository lmsAccountRepository;
    private final StudentRepository studentRepository;

    public LmsAccountService(LMSAccountRepository lmsAccountRepository, StudentRepository studentRepository) {
        this.lmsAccountRepository = lmsAccountRepository;
        this.studentRepository = studentRepository;
    }

    @Transactional
    public LMSAccount registerLmsAccount(Long studentId, String username) {
        Student s = studentRepository.findById(studentId).orElseThrow();
        // 1:1 보장: 있으면 업데이트, 없으면 생성
        LMSAccount acc = lmsAccountRepository.findByStudentId(studentId).orElseGet(() -> {
            LMSAccount a = new LMSAccount();
            a.setStudent(s);
            return a;
        });
        acc.setUsername(username);
        acc.setStatus("CONNECTED");
        return lmsAccountRepository.save(acc);
    }

    @Transactional
    public LMSAccount registerLmsAccount(Long studentId, String username, String password) {
        Student s = studentRepository.findById(studentId).orElseThrow();
        // 1:1 보장: 있으면 업데이트, 없으면 생성
        LMSAccount acc = lmsAccountRepository.findByStudentId(studentId).orElseGet(() -> {
            LMSAccount a = new LMSAccount();
            a.setStudent(s);
            return a;
        });
        acc.setUsername(username);
        acc.setPassword(password);
        acc.setStatus("CONNECTED");
        return lmsAccountRepository.save(acc);
    }

    @Transactional(readOnly = true)
    public LMSAccount getByStudentId(Long studentId) {
        return lmsAccountRepository.findByStudentId(studentId).orElseThrow();
    }
}


