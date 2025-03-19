package com.audora.inhash.controller;

import com.audora.inhash.model.ItContestSite;
import com.audora.inhash.service.ItContestSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/it-contest-sites")
@RequiredArgsConstructor
public class ItContestSiteController {
    private final ItContestSiteService itContestSiteService;

    @GetMapping
    public ResponseEntity<List<ItContestSite>> getAllSites() {
        return new ResponseEntity<>(itContestSiteService.getAllSites(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItContestSite> getSiteById(@PathVariable Long id) {
        ItContestSite site = itContestSiteService.getSiteById(id);
        return site != null ? new ResponseEntity<>(site, HttpStatus.OK) : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PostMapping
    public ResponseEntity<ItContestSite> createSite(@RequestBody ItContestSite site) {
        return new ResponseEntity<>(itContestSiteService.saveSite(site), HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSite(@PathVariable Long id) {
        itContestSiteService.deleteSite(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
