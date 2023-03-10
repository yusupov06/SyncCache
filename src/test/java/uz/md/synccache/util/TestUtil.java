package uz.md.synccache.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Assertions;
import uz.md.synccache.dtos.GetByDateRequest;
import uz.md.synccache.dtos.TransactionDTO;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.utils.MockGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class TestUtil {

    private static final ObjectMapper mapper = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    public static byte[] convertObjectToJsonBytes(Object object) throws IOException {
        return mapper.writeValueAsBytes(object);
    }

    public static byte[] createByteArray(int size, String data) {
        byte[] byteArray = new byte[size];
        for (int i = 0; i < size; i++) {
            byteArray[i] = Byte.parseByte(data, 2);
        }
        return byteArray;
    }


    public static <T> List<T> findAll(EntityManager em, Class<T> clss) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(clss);
        Root<T> rootEntry = cq.from(clss);
        CriteriaQuery<T> all = cq.select(rootEntry);
        TypedQuery<T> allQuery = em.createQuery(all);
        return allQuery.getResultList();
    }

    private TestUtil() {
    }


    public static void transactionAfterUpdateNotEquals(List<Transaction> actual, List<Transaction> expected) {
        Assertions.assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++) {
            Assertions.assertEquals(expected.get(i).getId(), actual.get(i).getId());
            Assertions.assertNotEquals(expected.get(i).getAmount(), actual.get(i).getAmount());
            Assertions.assertNotEquals(expected.get(i).getStatus(), actual.get(i).getStatus());
            Assertions.assertEquals(expected.get(i).getFromCard(), actual.get(i).getFromCard());
            Assertions.assertEquals(expected.get(i).getToCard(), actual.get(i).getToCard());
            Assertions.assertEquals(expected.get(i).getAddedDate(), actual.get(i).getAddedDate());
        }
    }

    public static void transactionsEquals(List<Transaction> actual, List<Transaction> expected) {
        assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++)
            transactionsEquals(actual.get(i), expected.get(i));
    }

    public static void transactionsEquals(Transaction actual, Transaction expected) {
        assertNotNull(actual);
        assertNotNull(expected);
//        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getFromCard(), actual.getFromCard());
        assertEquals(expected.getToCard(), actual.getToCard());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getAmount(), actual.getAmount());
    }

    public static void transactionAndDTOEquals(Transaction actual, TransactionDTO expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertEquals(expected.getFromCard(), actual.getFromCard());
        assertEquals(expected.getToCard(), actual.getToCard());
        assertEquals(expected.getAddedDate(), actual.getAddedDate());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getAmount(), actual.getAmount());
    }

    public static void transactionsAndDTOsEquals(List<Transaction> actual, List<TransactionDTO> expected) {
        assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++)
            transactionAndDTOEquals(actual.get(i), expected.get(i));
    }


    public static List<List<Transaction>> mockUzCardTransactions(List<Predicate<Transaction>> cardPredicates, Predicate<Transaction> datePredicate) {

        List<List<Transaction>> cardTransactions = new ArrayList<>();

        for (Predicate<Transaction> cardPredicate : cardPredicates) {
            cardTransactions.add(MockGenerator
                    .getUzCardTransactions().stream()
                    .filter(cardPredicate.and(datePredicate))
                    .toList());
        }

        return cardTransactions;
    }

    public static List<List<Transaction>> mockUzCardTransactionsWithDateTime(List<Predicate<Transaction>> cardPredicates, Predicate<Transaction> dateTimePredicate) {
        List<List<Transaction>> cardTransactions = new ArrayList<>();

        for (Predicate<Transaction> cardPredicate : cardPredicates) {
            cardTransactions.add(MockGenerator
                    .getUzCardTransactions().stream()
                    .filter(cardPredicate.and(dateTimePredicate))
                    .sorted(Comparator.comparing(Transaction::getAddedDate))
                    .toList());
        }

        return cardTransactions;
    }

    public static void transactionsAndDTOsEquals(List<List<Transaction>> transactions,
                                                 Map<String, List<TransactionDTO>> body,
                                                 GetByDateRequest request) {

        for (int i = 0; i < request.getCardNumbers().size(); i++) {
            transactionsAndDTOsEquals(transactions.get(i), body.get(request.getCardNumbers().get(i)));
        }

    }
}
