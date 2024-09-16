package com.app.cache;

import com.app.metrics.CustomCacheWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Slf4j
@Component
public class CacheUpdateListener {

    private final CustomCacheWrapper customCacheWrapper;

    @Autowired
    public CacheUpdateListener(CustomCacheWrapper customCacheWrapper) {
        this.customCacheWrapper = customCacheWrapper;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheUpdate(CacheUpdateEvent event) {
        log.info("Updating cache after transaction commit for countryCode: {}", event.getCountryCode());
        customCacheWrapper.put(event.getCountryCode(), event.getRequest());
    }
}