package uz.md.synccache.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.md.synccache.clientService.UzCardClient;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.utils.AppUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class UzCardGetTransactionStrategy implements GetTransactionsStrategy {

    private final UzCardClient uzCardClient;

    @Override
    public String getCardPrefix() {
        return "8600";
    }

    @Override
    public Map<String, List<Transaction>> getTransactionsBetweenDays(List<String> cards, LocalDateTime dateFrom, LocalDateTime dateTo) {

        if (cards == null || dateFrom == null || dateTo == null)
            throw new BadRequestException("Invalid request");

        LocalDate fromDate = dateFrom.toLocalDate();
        LocalDate toDate = dateTo.toLocalDate();

        Map<String, List<Transaction>> response = new HashMap<>();

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(dateFrom, dateTo);

        for (String card : cards) {
            List<Transaction> transactions = uzCardClient
                    .getTransactionsBetweenDates(card, fromDate, toDate);
            if (transactions != null && !transactions.isEmpty())
                response.put(card, transactions
                        .stream()
                        .filter(dateTimePredicate)
                        .sorted(Comparator.comparing(Transaction::getAddedDate))
                        .toList());
        }

        return response;
    }

}
