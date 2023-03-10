package uz.md.synccache.strategy;

import org.springframework.stereotype.Service;
import uz.md.synccache.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class HumoGetTransactionsStrategy implements GetTransactionsStrategy {

    @Override
    public String getCardPrefix() {
        return "9860";
    }

    @Override
    public Map<String, List<Transaction>> getTransactionsBetweenDays(List<String> cards, LocalDateTime dateFrom, LocalDateTime dateTo) {
        return null;
    }
}
