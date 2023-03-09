package uz.md.synccache.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.md.synccache.cache.MyCache;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
@ConditionalOnProperty(
        value="app.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CacheInvalidatorJob {

    private final MyCache cache;

    @Scheduled(fixedDelayString = "${cache.expire.delay.in}", timeUnit = TimeUnit.MINUTES)
    public void execute() {
        log.info("Executing Cache invalidator job");
        cache.invalidateAll();
    }
}
