package uz.md.synccache.utils;

import uz.md.synccache.entity.Transaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public static Map<String, List<String>> getCardsGroup(List<String> cards) {

        Map<String, List<String>> cardsGroup = new HashMap<>();

        for (String card : cards) {
            String prefix = card.substring(0, 4);
            List<String> list = cardsGroup.getOrDefault(prefix, new ArrayList<>());
            list.add(card);
            cardsGroup.put(prefix, list);
        }

        return cardsGroup;
    }

    public static List<Predicate<Transaction>> cardPredicates(List<String> cardNumbers) {
        List<Predicate<Transaction>> cardPredicates = new ArrayList<>();
        for (String cardNumber : cardNumbers) {
            cardPredicates.add(cardPredicate(cardNumber));
        }
        return cardPredicates;
    }
}
