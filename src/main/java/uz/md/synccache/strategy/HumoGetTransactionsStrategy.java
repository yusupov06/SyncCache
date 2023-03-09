package uz.md.synccache.strategy;

import org.springframework.stereotype.Service;
import uz.md.synccache.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HumoGetTransactionsStrategy implements GetTransactionsStrategy {

    @Override
    public String getCardPrefix() {
        return "9860";
    }

    @Override
    public List<Transaction> getTransactionsBetweenDays(String card, LocalDateTime dateFrom, LocalDateTime dateTo) {
        return null;
    }
}
