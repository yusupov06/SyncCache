package uz.md.synccache.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uz.md.synccache.service.TransactionService;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@EnableScheduling
@ConditionalOnProperty(
        value="app.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@Slf4j
public class CacheUpdaterJob {

    private final TransactionService transactionService;

    @Scheduled(fixedDelayString = "${cache.update.in}", timeUnit = TimeUnit.MINUTES)
    public void execute() {
        log.info("Executing Cache updater job");
        transactionService.checkForCachedDataAndUpdate();
    }
}
