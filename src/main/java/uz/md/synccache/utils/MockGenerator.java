package uz.md.synccache.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MockGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static Set<String> uzCards = new HashSet<>();
    private static Set<String> humoCards = new HashSet<>();

    @Setter
    private static List<Transaction> uzCardTransactions = new ArrayList<>();

    private static List<Transaction> humoTransactions = new ArrayList<>();

    // UzCard Mock

    public static void setUzCards(int count) {
        while (uzCards.size() < count) {
            String card = "8600" + RandomStringUtils.random(12, false, true);
            uzCards.add(card);
        }
    }

    public static List<Transaction> generateMockUzCardTransactions(int count) {

        List<Transaction> transactions = new ArrayList<>();

        Random random = new Random();

        for (int i = 0; i < count; i++) {

            double amount = Math.round(random.nextDouble() * 500) + 100.0;
            String[] cards = getTwoDifferentCards(getUzCards());
            transactions.add(Transaction.builder()
                    .fromCard(cards[0])
                    .toCard(cards[1])
                    .amount(BigDecimal.valueOf(amount))
                    .status(TransactionStatus.SUCCESS)
                    .addedDate(getRandomLocalDateTimeBetween(LocalDateTime.now().minusDays(10), LocalDateTime.now()))
                    .build());
        }
        return transactions;
    }

    public static List<Transaction> getUzCardTransactions() {
        return uzCardTransactions.stream()
                .map(transaction -> Transaction.builder()
                        .addedDate(transaction.getAddedDate())
                        .status(transaction.getStatus())
                        .addedDate(transaction.getAddedDate())
                        .fromCard(transaction.getFromCard())
                        .toCard(transaction.getToCard())
                        .amount(transaction.getAmount())
                        .build())
                .toList();
    }

    public static List<String> getUzCards() {
        return new ArrayList<>(uzCards);
    }


    public static List<Transaction> getHumoTransactions() {
        return humoTransactions;
    }

    private static String[] getTwoDifferentCards(List<String> cards) {
        int from = RandomUtils.nextInt(0, 8);
        int to = RandomUtils.nextInt(0, 8);

        while (from == to) {
            from = RandomUtils.nextInt(0, 8);
            to = RandomUtils.nextInt(0, 8);
        }

        String[] res = new String[2];
        res[0] = cards.get(from);
        res[1] = cards.get(to);
        return res;
    }

    public static LocalDateTime getRandomLocalDateTimeBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
        long startEpoch = dateFrom.toEpochSecond(ZoneOffset.UTC);
        long endEpoch = dateTo.toEpochSecond(ZoneOffset.UTC);
        long randomEpoch = ThreadLocalRandom.current().nextLong(startEpoch, endEpoch);
        return LocalDateTime.ofEpochSecond(randomEpoch, 0, ZoneOffset.UTC);
    }

}
