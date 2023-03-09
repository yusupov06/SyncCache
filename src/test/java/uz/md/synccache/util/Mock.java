//package uz.md.caffeinecachespringboot.util;
//
//import org.apache.commons.lang3.RandomStringUtils;
//import uz.md.caffeinecachespringboot.entity.Transaction;
//
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//import java.time.ZoneOffset;
//import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.ThreadLocalRandom;
//
//public class Mock {
//
//    private static Random random = new Random();
//
//    public static List<Transaction> generateTransactions(int count, Long idFrom, LocalDateTime dateFrom, LocalDateTime dateTo) {
//        List<Transaction> transactions = new ArrayList<>();
//        for (int i = 0; i < count; i++) {
//            transactions.add(generateOneTransaction(idFrom++, dateFrom, dateTo));
//        }
//        transactions.sort(Comparator.comparing(Transaction::getAddedDate));
//        return transactions;
//    }
//
//    private static Transaction generateOneTransaction(Long id, LocalDateTime dateFrom, LocalDateTime dateTo) {
//        LocalDateTime addedDate = getRandomLocalDateTimeBetween(dateFrom, dateTo);
//        return makeTransaction(id, addedDate);
//    }
//
//    public static Transaction makeTransaction(Long id, LocalDateTime addedDate) {
//
//        double amount = Math.round(random.nextDouble() * 500) + 100.0;
//        String fromCard = "8600" + RandomStringUtils.random(12, false, true);
//        String toCard = "9860" + RandomStringUtils.random(12, false, true);
//
//        return Transaction.builder()
//                .id(id)
//                .fromCard(fromCard)
//                .toCard(toCard)
//                .addedDate(addedDate)
//                .amount(BigDecimal.valueOf(amount))
//                .status("SUCCESS")
//                .build();
//    }
//
//    public static LocalDateTime getRandomLocalDateTimeBetween(LocalDateTime dateFrom, LocalDateTime dateTo) {
//        long startEpoch = dateFrom.toEpochSecond(ZoneOffset.UTC);
//        long endEpoch = dateTo.toEpochSecond(ZoneOffset.UTC);
//        long randomEpoch = ThreadLocalRandom.current().nextLong(startEpoch, endEpoch);
//        return LocalDateTime.ofEpochSecond(randomEpoch, 0, ZoneOffset.UTC);
//    }
//
//
//}
