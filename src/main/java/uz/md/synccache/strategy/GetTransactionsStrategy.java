package uz.md.synccache.strategy;

import uz.md.synccache.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public interface GetTransactionsStrategy {
    String getCardPrefix();
    List<Transaction> getTransactionsBetweenDays(String card, LocalDateTime dateFrom, LocalDateTime dateTo);
}
