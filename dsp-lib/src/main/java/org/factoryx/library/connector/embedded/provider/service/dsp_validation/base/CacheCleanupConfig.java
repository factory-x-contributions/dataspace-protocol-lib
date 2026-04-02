package org.factoryx.library.connector.embedded.provider.service.dsp_validation.base;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Optional;

@Configuration
@EnableScheduling
public class CacheCleanupConfig {


    private final BaseDcpValidationService validationService;

    public CacheCleanupConfig(Optional<BaseDcpValidationService> validationService) {
        this.validationService = validationService.orElse(null);
    }

    @Scheduled(fixedRateString = "${org.factoryx.library.dcpvalidation.cacheupdate.interval:24h}")
    public void cleanupCache() {
        if (validationService != null) {
            validationService.doCleanups();
        }
    }
}
