package uz.md.synccache.service;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
import uz.md.synccache.util.TestUtil;
import uz.md.synccache.utils.AppUtils;
import uz.md.synccache.utils.MockGenerator;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.mockito.Mockito.when;
import static uz.md.synccache.util.TestUtil.transactionsAndDTOsEquals;
import static uz.md.synccache.util.TestUtil.transactionsEquals;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class UzCardGetTransactionsTest {

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private UzCardClient uzCardClient;

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

    /**
     * Should get from client with list of cards
     */
    @Test
    void shouldGetWithListOfCardsFromClientService() {

        List<String> cards = MockGenerator.getUzCards().subList(0, 2);
        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();
        GetByDateRequest request = new GetByDateRequest(cards, fromDate, toDate);

        setThisRequestAndCheckIt(request);

    }

    /**
     * Should get from cache after cached
     */
    @Test
    void shouldGetFromCache() {
        List<String> cards = MockGenerator.getUzCards().subList(0, 2);
        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();
        GetByDateRequest request = new GetByDateRequest(cards, fromDate, toDate);

        setThisRequestAndCheckIt(request);

        GetByDateRequest request2 = new GetByDateRequest(cards, fromDate, toDate);

        ResponseEntity<Map<String, List<TransactionDTO>>> response = transactionService
                .getByDateBetween(request2);

        // Check for call to client
        callToClient(request);

        Assertions.assertNotNull(response);
        Map<String, List<TransactionDTO>> body = response.getBody();
        Assertions.assertNotNull(body);


        List<Predicate<Transaction>> cardPredicates = AppUtils
                .cardPredicates(request.getCardNumbers());

        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo());

        List<List<Transaction>> mockUzCardTransactionsAfterCall = TestUtil.mockUzCardTransactionsWithDateTime(cardPredicates, dateTimePredicate);

        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body, request);
    }

    /**
     * should get from cache and client
     */
    @Test
    void shouldGetFromCacheAndClient() {

        List<String> cards = MockGenerator.getUzCards().subList(0, 2);
        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();
        GetByDateRequest request = new GetByDateRequest(cards, fromDate, toDate);

        // first call
        setThisRequestAndCheckIt(request);

        String card = MockGenerator.getUzCards().get(4);
        cards.add(card);

        GetByDateRequest request2 = new GetByDateRequest(cards, fromDate, toDate);

        Predicate<Transaction> datePredicate = AppUtils
                .datePredicate(fromDate.toLocalDate(), toDate.toLocalDate());

        List<Transaction> transactions = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(AppUtils.cardPredicate(card).and(datePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(card, fromDate.toLocalDate(), toDate.toLocalDate()))
                .thenReturn(transactions);

        ResponseEntity<Map<String, List<TransactionDTO>>> response = transactionService
                .getByDateBetween(request2);

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(0), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(1), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumbers().get(2), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        Assertions.assertNotNull(response);
        Map<String, List<TransactionDTO>> body = response.getBody();
        Assertions.assertNotNull(body);

        List<Predicate<Transaction>> cardPredicates = AppUtils
                .cardPredicates(request.getCardNumbers());

        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo());

        List<List<Transaction>> mockUzCardTransactionsAfterCall = TestUtil.mockUzCardTransactionsWithDateTime(cardPredicates, dateTimePredicate);

        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body, request);
    }


    void setThisRequestAndCheckIt(GetByDateRequest request) {

        List<Predicate<Transaction>> cardPredicates = AppUtils
                .cardPredicates(request.getCardNumbers());

        Predicate<Transaction> datePredicate = AppUtils
                .datePredicate(request.getDateFrom().toLocalDate(),
                        request.getDateTo().toLocalDate());

        List<List<Transaction>> mockUzCardTransactions = TestUtil.mockUzCardTransactions(cardPredicates, datePredicate);

        // check for cache is empty
        isCacheEmpty(request);

        // When transaction
        whenMockReturn(request, mockUzCardTransactions);

        // First call and save to cache
        ResponseEntity<Map<String, List<TransactionDTO>>> responseEntity = transactionService
                .getByDateBetween(request);

        // Verify that call to client
        callToClient(request);

        // Check for cached
        checkForCorrectlyCached(request);

        Assertions.assertNotNull(responseEntity);
        Map<String, List<TransactionDTO>> body = responseEntity.getBody();
        Assertions.assertNotNull(body);

        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo());

        List<List<Transaction>> mockUzCardTransactionsAfterCall = TestUtil.mockUzCardTransactionsWithDateTime(cardPredicates, dateTimePredicate);

        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body, request);
    }

    private void checkForCorrectlyCached(GetByDateRequest request) {
        for (String cardNumber : request.getCardNumbers()) {
            checkForCorrectlyCached(cardNumber, request.getDateFrom(), request.getDateTo());
        }
    }

    private void callToClient(GetByDateRequest request) {
        for (String cardNumber : request.getCardNumbers()) {
            // Check for call to client
            Mockito.verify(uzCardClient, Mockito.times(1))
                    .getTransactionsBetweenDates(cardNumber, request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());
        }
    }

    private void whenMockReturn(GetByDateRequest request, List<List<Transaction>> transactions) {
        int k = 0;
        for (String cardNumber : request.getCardNumbers()) {
            when(uzCardClient.getTransactionsBetweenDates(cardNumber, request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate()))
                    .thenReturn(transactions.get(k++));
        }
    }

    private void isCacheEmpty(GetByDateRequest request) {
        for (String cardNumber : request.getCardNumbers()) {
            Assertions.assertTrue(myCache.isEmpty(cardNumber, request.getDateFrom(), request.getDateTo()));
        }
    }

    private void checkForCorrectlyCached(String card, LocalDateTime dateFrom, LocalDateTime dateTo) {


        // check for response is cached
        Assertions.assertFalse(myCache
                .isEmpty(card, dateFrom, dateTo));

        // check for correctly cached
        List<Transaction> fromCache = myCache
                .getAllBetween(card, dateFrom, dateTo);

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(dateFrom,
                        dateTo);

        List<Transaction> mockUzCardTransactionsAfterCall = MockGenerator
                .getUzCardTransactions().stream()
                .filter(AppUtils.cardPredicate(card).and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(fromCache, mockUzCardTransactionsAfterCall);

        RangeDTO cacheRange = myCache.getCacheRange(card);
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(card, cacheRange.getCardNumber());
        Assertions.assertEquals(dateFrom, cacheRange.getFromDate());
        Assertions.assertEquals(dateTo, cacheRange.getToDate());

    }

}
