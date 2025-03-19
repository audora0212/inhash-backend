package com.audora.inhash.service;

import com.audora.inhash.model.InternshipInfo;
import com.audora.inhash.repository.InternshipInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InternshipInfoService {
    private final InternshipInfoRepository internshipInfoRepository;

    public List<InternshipInfo> getAllInternshipInfos() {
        return internshipInfoRepository.findAll();
    }

    public InternshipInfo saveInternshipInfo(InternshipInfo info) {
        return internshipInfoRepository.save(info);
    }

    public InternshipInfo getInternshipInfoById(Long id) {
        return internshipInfoRepository.findById(id).orElse(null);
    }

    public void deleteInternshipInfo(Long id) {
        internshipInfoRepository.deleteById(id);
    }
}
