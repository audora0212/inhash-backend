package com.audora.inhash.service;

import com.audora.inhash.model.SwNotice;
import com.audora.inhash.repository.SwNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SwNoticeService {
    private final SwNoticeRepository swNoticeRepository;

    public List<SwNotice> getAllNotices() {
        return swNoticeRepository.findAll();
    }

    public SwNotice saveNotice(SwNotice notice) {
        return swNoticeRepository.save(notice);
    }

    public SwNotice getNoticeById(Long id) {
        return swNoticeRepository.findById(id).orElse(null);
    }

    public void deleteNotice(Long id) {
        swNoticeRepository.deleteById(id);
    }
}
