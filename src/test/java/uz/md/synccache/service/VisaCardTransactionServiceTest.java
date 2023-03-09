package uz.md.synccache.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.cache.MyCache;
import uz.md.synccache.clientService.UzCardClient;
import uz.md.synccache.clientService.VisaCardClient;
import uz.md.synccache.dtos.GetByDateRequest;
import uz.md.synccache.dtos.RangeDTO;
import uz.md.synccache.dtos.TransactionDTO;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.entity.TransactionStatus;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.utils.AppUtils;
import uz.md.synccache.utils.MockGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class VisaCardTransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private VisaCardClient visaCardClient;

    @Autowired
    private MyCache myCache;

    @BeforeEach
    void init() {
        // clear the cache
        myCache.invalidateAll();
        myCache.deleteAllRanges();
    }

    // ################################# Visa Tests ###################################


    /**
     * Should get from visa client
     */
    @Test
    void shouldGetFromVisaClientByDateBetween() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> visaCards = MockGenerator.getVisaCards();

        GetByDateRequest request = new GetByDateRequest(visaCards.get(0), fromDate, toDate);

        setThisRequestAndCheckItInVisaCard(request);

    }


    /**
     * Common method that call to client with this request and check it
     * 1 - cached correctly
     * 2 - cache range set correctly
     * 3 - response getting correctly
     *
     * @param request - CardNumber, From and To date
     */
    private void setThisRequestAndCheckItInVisaCard(GetByDateRequest request) {

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request.getCardNumber());

        Predicate<Transaction> datePredicate = AppUtils
                .datePredicate(request.getDateFrom().toLocalDate(),
                        request.getDateTo().toLocalDate());

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request.getDateTo());

        List<Transaction> mockVisaCardTransactions = MockGenerator
                .getVisaCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate))
                .toList();

        // check for cache is empty
        Assertions.assertTrue(myCache.isEmpty(request.getCardNumber(), request.getDateFrom(), request.getDateTo()));

        when(visaCardClient.getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate()))
                .thenReturn(mockVisaCardTransactions);

        // First call and save to cache
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request);

        // Check for call to client
        Mockito.verify(visaCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // check for response is cached
        Assertions.assertFalse(myCache
                .isEmpty(request.getCardNumber(), request.getDateFrom(), request.getDateTo()));

        // check for correctly cached
        List<Transaction> fromCache = myCache
                .getAllBetween(request.getCardNumber(), request.getDateFrom(), request.getDateTo());

        List<Transaction> mockVisaCardTransactionsAfterCall = mockVisaCardTransactions
                .stream()
                .filter(dateTimePredicate)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(fromCache, mockVisaCardTransactionsAfterCall);

        RangeDTO cacheRange = myCache.getCacheRange(request.getCardNumber());
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(request.getCardNumber(), cacheRange.getCardNumber());
        Assertions.assertEquals(request.getDateFrom(), cacheRange.getFromDate());
        Assertions.assertEquals(request.getDateTo(), cacheRange.getToDate());

        Assertions.assertNotNull(responseEntity);
        List<TransactionDTO> body = responseEntity.getBody();
        Assertions.assertNotNull(body);

        transactionsAndDTOsEquals(mockVisaCardTransactionsAfterCall, body);

    }


    private void transactionAfterUpdateNotEquals(List<Transaction> actual, List<Transaction> expected) {
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

    private void transactionsEquals(List<Transaction> actual, List<Transaction> expected) {
        assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++)
            transactionsEquals(actual.get(i), expected.get(i));
    }

    private void transactionsEquals(Transaction actual, Transaction expected) {
        assertNotNull(actual);
        assertNotNull(expected);
//        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getFromCard(), actual.getFromCard());
        assertEquals(expected.getToCard(), actual.getToCard());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getAmount(), actual.getAmount());
    }

    private void transactionAndDTOEquals(Transaction actual, TransactionDTO expected) {
        assertNotNull(actual);
        assertNotNull(expected);
        assertEquals(expected.getFromCard(), actual.getFromCard());
        assertEquals(expected.getToCard(), actual.getToCard());
        assertEquals(expected.getAddedDate(), actual.getAddedDate());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getAmount(), actual.getAmount());
    }

    private void transactionsAndDTOsEquals(List<Transaction> actual, List<TransactionDTO> expected) {
        assertEquals(actual.size(), expected.size());
        for (int i = 0; i < actual.size(); i++)
            transactionAndDTOEquals(actual.get(i), expected.get(i));
    }


}
