package com.audora.inhash.service;

import com.audora.inhash.model.ItContestSite;
import com.audora.inhash.repository.ItContestSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItContestSiteService {
    private final ItContestSiteRepository itContestSiteRepository;

    public List<ItContestSite> getAllSites() {
        return itContestSiteRepository.findAll();
    }

    public ItContestSite saveSite(ItContestSite site) {
        return itContestSiteRepository.save(site);
    }

    public ItContestSite getSiteById(Long id) {
        return itContestSiteRepository.findById(id).orElse(null);
    }

    public void deleteSite(Long id) {
        itContestSiteRepository.deleteById(id);
    }
}
