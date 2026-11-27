package com.nagarbondhu.config;

import com.nagarbondhu.pattern.singleton.TrackingIdGenerator;
import com.nagarbondhu.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Synchronizes the {@link TrackingIdGenerator} singleton counter with the highest
 * existing complaint sequence stored in PostgreSQL on application startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TrackingIdInitializer implements ApplicationRunner {

    private final ComplaintRepository complaintRepository;

    @Override
    @Transactional(readOnly = true)
    public void run(ApplicationArguments args) {
        List<String> trackingNumbers = complaintRepository.findAllTrackingNumbers();
        long maxSequence = 0L;

        for (String trackingNo : trackingNumbers) {
            if (trackingNo == null || trackingNo.isBlank()) {
                continue;
            }
            int lastDash = trackingNo.lastIndexOf('-');
            if (lastDash < 0 || lastDash == trackingNo.length() - 1) {
                continue;
            }
            String suffix = trackingNo.substring(lastDash + 1).trim();
            try {
                long sequence = Long.parseLong(suffix);
                if (sequence > maxSequence) {
                    maxSequence = sequence;
                }
            } catch (NumberFormatException ex) {
                log.debug("Ignoring non-numeric tracking number suffix in '{}'", trackingNo);
            }
        }

        TrackingIdGenerator.getInstance().syncCounter(maxSequence);
        log.info("TrackingIdGenerator synchronized at startup with maxExisting={}", maxSequence);
    }
}
