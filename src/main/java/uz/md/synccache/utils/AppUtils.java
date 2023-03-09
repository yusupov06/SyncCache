package uz.md.synccache.utils;

import uz.md.synccache.entity.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.function.Predicate;

public class AppUtils {

    public static Predicate<Transaction> datePredicate(LocalDate fromDate, LocalDate toDate) {
        return  transaction -> transaction.getAddedDate().toLocalDate().isAfter(fromDate.minusDays(1))
                && transaction.getAddedDate().toLocalDate().isBefore(toDate.plusDays(1));
    }

    public static Predicate<Transaction> dateTimePredicate(LocalDateTime fromDate, LocalDateTime toDate) {
        return  transaction -> transaction.getAddedDate().isAfter(fromDate.minusNanos(1))
                && transaction.getAddedDate().isBefore(toDate.plusNanos(1));
    }

     public static Predicate<Transaction> cardPredicate(String cardNumber) {
         return transaction -> transaction.getFromCard().equals(cardNumber)
                 || transaction.getToCard().equals(cardNumber);
     }

}
