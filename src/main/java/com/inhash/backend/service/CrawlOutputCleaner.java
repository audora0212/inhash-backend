package com.inhash.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public class CrawlOutputCleaner {

    @Value("${inhash.crawl.python.workingDir:}")
    private String workingDir;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanOnStartup() {
        try {
            if (workingDir == null || workingDir.isBlank()) return;
            Path outputDir = Path.of(workingDir, "output");
            if (Files.exists(outputDir)) {
                try (Stream<Path> s = Files.walk(outputDir)) {
                    s.sorted(Comparator.reverseOrder())
                        .filter(p -> !p.equals(outputDir))
                        .forEach(p -> {
                            try { Files.deleteIfExists(p); } catch (Exception ignore) {}
                        });
                }
            }
            Files.createDirectories(outputDir);
            System.out.println("[CrawlOutputCleaner] Cleaned output directory: " + outputDir);
        } catch (Exception e) {
            System.out.println("[CrawlOutputCleaner] Cleanup failed: " + e.getMessage());
        }
    }
}


