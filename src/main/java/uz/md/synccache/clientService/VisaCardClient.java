package uz.md.synccache.clientService;

import org.springframework.stereotype.Component;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.utils.AppUtils;
import uz.md.synccache.utils.MockGenerator;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

@Component
public class VisaCardClient {

    public List<Transaction> getTransactionsBetweenDates(String cardNumber, LocalDate fromDate, LocalDate toDate) {

        Predicate<Transaction> cardPredicate = AppUtils.cardPredicate(cardNumber);
        Predicate<Transaction> datePredicate = AppUtils.datePredicate(fromDate, toDate);

        return MockGenerator.getVisaCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate))
                .toList();
    }
}
