package com.aicourse.scheduler;

import com.aicourse.repo.LessonRepo;
import com.aicourse.service.courses.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import com.aicourse.dto.GenerationLog;

/**
 * Background scheduler that auto-generates lesson content for un-enriched lessons.
 * Can be toggled on/off at runtime via the REST API (LLM Operations UI).
 */
@Component
public class LessonEnrichmentScheduler {

    private static final Logger LOGGER = Logger.getLogger(LessonEnrichmentScheduler.class.getName());

    @Autowired
    private LessonService lessonService;

    @Autowired
    private LessonRepo lessonRepo;

    // --- Runtime-toggleable state ---
    private final AtomicBoolean enabled = new AtomicBoolean(false);
    private final AtomicInteger batchSize = new AtomicInteger(2);
    private final AtomicLong intervalMs = new AtomicLong(60_000);

    // --- Stats ---
    private final AtomicLong totalGenerated = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong lastRunTimestamp = new AtomicLong(0);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile String lastError = null;
    
    // --- Audit Logs ---
    private final ConcurrentLinkedDeque<GenerationLog> recentLogs = new ConcurrentLinkedDeque<>();
    private static final int MAX_LOGS = 100;

    @Scheduled(fixedDelay = 15_000) // checks every 15 seconds
    public void tick() {
        if (!enabled.get()) return;

        long now = System.currentTimeMillis();
        long lastRun = lastRunTimestamp.get();
        if (now - lastRun < intervalMs.get()) return;

        if (!running.compareAndSet(false, true)) return; // already running

        lastRunTimestamp.set(now);
        try {
            List<GenerationLog> logs = lessonService.enrichPendingLessonsLimited(batchSize.get());
            
            for (GenerationLog log : logs) {
                if (log.success()) {
                    incrementGenerated();
                } else {
                    incrementFailed();
                }
                recentLogs.addFirst(log);
                if (recentLogs.size() > MAX_LOGS) {
                    recentLogs.pollLast();
                }
            }
            lastError = null;
        } catch (Exception e) {
            lastError = e.getMessage();
            LOGGER.log(Level.SEVERE, "Auto-generation job failed: {0}", e.getMessage());
        } finally {
            running.set(false);
        }
    }

    // --- Controls ---
    public void setEnabled(boolean value) {
        boolean prev = enabled.getAndSet(value);
        if (value && !prev) {
            lastRunTimestamp.set(0); // trigger immediately
            LOGGER.log(Level.INFO, "Lesson auto-generation ENABLED");
        } else if (!value && prev) {
            LOGGER.log(Level.INFO, "Lesson auto-generation DISABLED");
        }
    }

    public boolean isEnabled() { return enabled.get(); }

    public void setBatchSize(int size) { batchSize.set(Math.max(1, Math.min(size, 10))); }
    public int getBatchSize() { return batchSize.get(); }

    public void setIntervalMs(long ms) { intervalMs.set(Math.max(10_000, ms)); }
    public long getIntervalMs() { return intervalMs.get(); }

    public boolean isRunning() { return running.get(); }
    public long getTotalGenerated() { return totalGenerated.get(); }
    public long getTotalFailed() { return totalFailed.get(); }
    public String getLastError() { return lastError; }
    public long getLastRunTimestamp() { return lastRunTimestamp.get(); }

    public void incrementGenerated() { totalGenerated.incrementAndGet(); }
    public void incrementFailed() { totalFailed.incrementAndGet(); }

    public long getPendingCount() {
        try {
            return lessonRepo.countByIsEnriched(false);
        } catch (Exception e) {
            return -1;
        }
    }
    
    public List<GenerationLog> getRecentLogs() {
        return List.copyOf(recentLogs);
    }
}
