package uz.md.synccache.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uz.md.synccache.clientService.VisaCardClient;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.utils.AppUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class VisaCardGetTransactionsStrategy implements GetTransactionsStrategy {

    private final VisaCardClient visaCardClient;

    @Override
    public String getCardPrefix() {
        return "5555";
    }

    @Override
    public List<Transaction> getTransactionsBetweenDays(String card, LocalDateTime dateFrom, LocalDateTime dateTo) {

        if (card == null || dateFrom == null || dateTo == null)
            throw new BadRequestException("Invalid request");

        LocalDate fromDate = dateFrom.toLocalDate();
        LocalDate toDate = dateTo.toLocalDate();

        List<Transaction> response = visaCardClient
                .getTransactionsBetweenDates(card, fromDate, toDate);

        if (response == null)
            return new ArrayList<>();

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(dateFrom, dateTo);

        return response.stream()
                .filter(dateTimePredicate)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();
    }
}
