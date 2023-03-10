package uz.md.synccache.strategy;

import uz.md.synccache.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface GetTransactionsStrategy {

    String getCardPrefix();

    Map<String, List<Transaction>> getTransactionsBetweenDays(List<String> cards, LocalDateTime dateFrom, LocalDateTime dateTo);
}
