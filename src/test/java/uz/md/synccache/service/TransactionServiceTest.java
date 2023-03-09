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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @MockBean
    private UzCardClient uzCardClient;

    @Autowired
    private MyCache myCache;

    @BeforeEach
    void init() {
        // clear the cache
        myCache.invalidateAll();
        myCache.deleteAllRanges();
    }

    // Get by between dates  tests

    @Test
    void shouldThrowBadRequestExceptionByDateBetweenWithNullDateFrom() {
        GetByDateRequest request = new GetByDateRequest("86003129", null, LocalDateTime.now());
        assertThrows(BadRequestException.class, () -> transactionService.getByDateBetween(request));

        Assertions.assertTrue(myCache.isEmpty());
    }

    @Test
    void shouldThrowBadRequestExceptionByDateBetweenWithNullCardNumber() {
        GetByDateRequest request = new GetByDateRequest(null, LocalDateTime.now().minusDays(5), LocalDateTime.now());
        assertThrows(BadRequestException.class, () -> transactionService.getByDateBetween(request));

        // Check for not to call to client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        Assertions.assertTrue(myCache.isEmpty());
    }

    @Test
    void shouldThrowBadRequestExceptionByDateBetweenWithNullDateTo() {
        GetByDateRequest request = new GetByDateRequest("86003129", LocalDateTime.now().minusDays(2), null);
        assertThrows(BadRequestException.class, () -> transactionService.getByDateBetween(request));

        Assertions.assertTrue(myCache.isEmpty());
    }

    @Test
    void shouldThrowBadRequestExceptionByDateBetweenWithNullRequest() {
        assertThrows(BadRequestException.class, () -> transactionService.getByDateBetween(null));
        Assertions.assertTrue(myCache.isEmpty());
    }

    /**
     * Should get from uzcard client
     */
    @Test
    void shouldGetFromClientByDateBetween() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

    }

    /**
     * Should get from cache after cached
     * [1-10] in uzcard client
     * We called [2-6]
     * And we call again this range we get from cache
     */
    @Test
    void shouldGetFromCache() {
        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request);

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);


        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(), request.getDateTo());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request.getCardNumber());

        List<Transaction> mockUzCardTransactionsAfterCall = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // response body and client service data equality
        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body);
    }

    /**
     * Should get from cache after cached
     * [1-10] in uzcard client
     * We called [1-6]
     * And we call [3-5] range this range should be got from cache
     */
    @Test
    void shouldGetFromInsideCache() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // Second request
        GetByDateRequest request2 = new GetByDateRequest(request.getCardNumber(), LocalDateTime.now().minusDays(5),
                LocalDateTime.now().minusDays(3));

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request2.getDateFrom(), request2.getDateTo());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumber());

        List<Transaction> mockUzCardTransactionsAfterCall = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();


        Predicate<Transaction> predicate = AppUtils.dateTimePredicate(request2.getDateFrom(), request2.getDateTo());

        // After second call result should be like this
        List<Transaction> transactionsAfterSecondCall = mockUzCardTransactionsAfterCall
                .stream()
                .filter(predicate)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // Second call with [3-5]
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request2);

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(transactionsAfterSecondCall, body);
    }

    /**
     * Should get from cache after cached
     * [1-10] in uzcard client
     * [5-7] is cached
     * And we call [1-3] range, and [1-4] should be called to client
     */
    @Test
    void shouldGetFromClientWithCachedAnotherRange() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(8);
        LocalDateTime toDate = LocalDateTime.now().minusDays(6);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // ################### Second request #################

        GetByDateRequest request2 = new GetByDateRequest(request.getCardNumber(),
                LocalDateTime.now().minusDays(3),
                LocalDateTime.now());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumber());

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request.getDateTo().toLocalDate(),
                        request2.getDateTo().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumber(),
                request.getDateTo().toLocalDate(),
                request2.getDateTo().toLocalDate()))
                .thenReturn(mockUzCardTransactionsSecondCall);

        // Second call with [1-3]
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request2);

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumber(), request.getDateTo().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for cache range set correctly
        RangeDTO cacheRange = myCache.getCacheRange(request2.getCardNumber());
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(cacheRange.getFromDate(), request.getDateFrom());
        Assertions.assertEquals(cacheRange.getToDate(), request2.getDateTo());

        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request2.getDateTo());

        List<Transaction> fromCache = myCache
                .getAllBetween(request2.getCardNumber(), request.getDateFrom(), request2.getDateTo());

        // after call result should be like this
        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(AppUtils.cardPredicate(request2.getCardNumber()).and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        transactionsEquals(cacheResultShouldBe, fromCache);

        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        Predicate<Transaction> dateTimePredicate1 = AppUtils
                .dateTimePredicate(request2.getDateFrom(), request2.getDateTo());

        List<Transaction> resultShouldBe = cacheResultShouldBe
                .stream()
                .filter(dateTimePredicate1)
                .toList();

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body);

    }

    /**
     * Should get from cache after cached
     * [1-10] in uzcard client
     * [1-4] is cached
     * And we call [6-8] range this range should be called to client
     * And cache update with range [1-8]
     */
    @Test
    void shouldGetFromClientWithCachedAnotherRange2() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(4);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // ################### Second request #################

        GetByDateRequest request2 = new GetByDateRequest(request.getCardNumber(),
                LocalDateTime.now().minusDays(8),
                LocalDateTime.now().minusDays(6));

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumber());

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall = MockGenerator
                .getUzCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumber(),
                request2.getDateFrom().toLocalDate(),
                request.getDateFrom().toLocalDate()))
                .thenReturn(mockUzCardTransactionsSecondCall);

        // Second call with [6-8]
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request2);

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request.getDateFrom().toLocalDate());

        // Check for range set correctly
        RangeDTO cacheRange = myCache.getCacheRange(request2.getCardNumber());
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(cacheRange.getFromDate(), request2.getDateFrom());
        Assertions.assertEquals(cacheRange.getToDate(), request.getDateTo());


        // Check for correctly cached transactions

        List<Transaction> fromCache2 = myCache
                .getAllBetween(request.getCardNumber(), request2.getDateFrom(), request.getDateTo());

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(cacheResultShouldBe, fromCache2);


        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate2 = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        // after call result should be like this
        List<Transaction> resultShouldBe = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body);

    }

    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [5-8] is cached
     * And we call [2-7] range [5-7] from cache and [2-4] from uzcard
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClient() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        // Imagine now 15
        // FromDate = 11
        LocalDateTime fromDate = LocalDateTime.now().minusDays(4);
        // ToDate = 15
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // ################### Second request #################

        // FromDate = 9
        // ToDate = 13
        GetByDateRequest request2 = new GetByDateRequest(request.getCardNumber(),
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now().minusDays(2));

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumber());

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumber(),
                request2.getDateFrom().toLocalDate(),
                request.getDateFrom().toLocalDate()))
                .thenReturn(mockUzCardTransactionsSecondCall);

        // Second call
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request2);

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request.getDateFrom().toLocalDate());

        RangeDTO cacheRange = myCache.getCacheRange(request2.getCardNumber());
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(cacheRange.getFromDate(), request2.getDateFrom());
        Assertions.assertEquals(cacheRange.getToDate(), request.getDateTo());


        // Check for updated cache
        List<Transaction> fromCache2 = myCache
                .getAllBetween(request.getCardNumber(), request2.getDateFrom(), request.getDateTo());


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(cacheResultShouldBe, fromCache2);

        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate2 = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        // after call result should be like this
        List<Transaction> resultShouldBe = cacheResultShouldBe.stream()
                .filter(dateTimePredicate2)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body);

    }

    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [5-8] is cached
     * And we call [2-7] range [5-7] from cache and [2-4] from uzcard
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClientButClientResponseIsEmpty() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        // Imagine now 15
        // FromDate = 11
        LocalDateTime fromDate = LocalDateTime.now().minusDays(4);
        // ToDate = 15
        LocalDateTime toDate = LocalDateTime.now();

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // ################### Second request #################

        // FromDate = 9
        // ToDate = 13
        GetByDateRequest request2 = new GetByDateRequest(request.getCardNumber(),
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now().minusDays(2));

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumber());

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().toLocalDate());



        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumber(),
                request2.getDateFrom().toLocalDate(),
                request.getDateFrom().toLocalDate()))
                .thenReturn(new ArrayList<>());

        // Second call
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request2);

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request.getDateFrom().toLocalDate());

        RangeDTO cacheRange = myCache.getCacheRange(request2.getCardNumber());
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(cacheRange.getFromDate(), request.getDateFrom());
        Assertions.assertEquals(cacheRange.getToDate(), request.getDateTo());


        // Check for updated cache
        List<Transaction> fromCache2 = myCache
                .getAllBetween(request.getCardNumber(), request2.getDateFrom(), request.getDateTo());


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(cacheResultShouldBe, fromCache2);

        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate2 = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        // after call result should be like this
        List<Transaction> resultShouldBe = cacheResultShouldBe.stream()
                .filter(dateTimePredicate2)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body);

    }

    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [1-4] is cached
     * And we call [2-8] range [2-4] from cache and [5-8] from uzcard
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClient2() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        // Imagine now 8
        // FromDate = 1
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        // ToDate = 4
        LocalDateTime toDate = LocalDateTime.now().minusDays(4);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // ################### Second request #################


        // Imagine now is 8
        // FromDate = 2
        // ToDate = 8
        GetByDateRequest request2 = new GetByDateRequest(request.getCardNumber(),
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumber());

        // in Second call with dates
        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumber(),
                request.getDateTo().plusNanos(1).toLocalDate(),
                request2.getDateTo().toLocalDate()))
                .thenReturn(mockUzCardTransactionsSecondCall);

        // Second call
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request2);

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumber(), request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        RangeDTO cacheRange = myCache.getCacheRange(request2.getCardNumber());
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(cacheRange.getFromDate(), request.getDateFrom());
        Assertions.assertEquals(cacheRange.getToDate(), request2.getDateTo());


        // Check for updated cache
        List<Transaction> fromCache2 = myCache
                .getAllBetween(request.getCardNumber(), request.getDateFrom(), request2.getDateTo());


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request2.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(cacheResultShouldBe, fromCache2);

        // Check for correctly cached transactions
        Predicate<Transaction> dateTimePredicate2 = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        // after call result should be like this
        List<Transaction> resultShouldBe = cacheResultShouldBe.stream()
                .filter(dateTimePredicate2)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body);

    }


    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [1-4] is cached
     * And we call [2-8] range [2-4] from cache and [5-8] from uzcard but result is null
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClientWithEmptyResultFromClient() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        // Imagine now 8
        // FromDate = 1
        LocalDateTime fromDate = LocalDateTime.now().minusDays(7);
        // ToDate = 4
        LocalDateTime toDate = LocalDateTime.now().minusDays(4);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // ################### Second request #################


        // Imagine now is 8
        // FromDate = 2
        // ToDate = 8
        GetByDateRequest request2 = new GetByDateRequest(request.getCardNumber(),
                LocalDateTime.now().minusDays(6),
                LocalDateTime.now());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumber());

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumber(),
                request.getDateTo().plusNanos(1).toLocalDate(),
                request2.getDateTo().toLocalDate()))
                .thenReturn(new ArrayList<>());

        // Second call
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request2);

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request2.getCardNumber(), request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        RangeDTO cacheRange = myCache.getCacheRange(request2.getCardNumber());
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(cacheRange.getFromDate(), request.getDateFrom());
        Assertions.assertEquals(cacheRange.getToDate(), request.getDateTo());

        // Check for updated cache
        List<Transaction> fromCache2 = myCache
                .getAllBetween(request.getCardNumber(), request.getDateFrom(), request2.getDateTo());


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(cacheResultShouldBe, fromCache2);

        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        Predicate<Transaction> dateTimePredicate1 = AppUtils
                .dateTimePredicate(request2.getDateFrom(), request2.getDateTo());

        List<Transaction> resultShouldBe = cacheResultShouldBe
                .stream()
                .filter(dateTimePredicate1)
                .toList();

        // response body and client service data equality
        transactionsAndDTOsEquals(resultShouldBe, body);

    }


    /**
     * Should get from cache and client
     * [1-10] in uzcard client
     * [3-5] is cached
     * And we call [1-8] range [3-5] from cache and [1-2] and [6-8] from uzcard
     */
    @Test
    void shouldGetSomeFromCacheAndSomeFromUzCardClient3() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        // Imagine now 8
        // FromDate = 3
        LocalDateTime fromDate = LocalDateTime.now().minusDays(5);
        // ToDate = 5
        LocalDateTime toDate = LocalDateTime.now().minusDays(3);

        // Real results

        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // ################### Second request #################

        // Imagine now is 8
        // FromDate = 1
        // ToDate = 8
        GetByDateRequest request2 = new GetByDateRequest(request.getCardNumber(),
                LocalDateTime.now().minusDays(7),
                LocalDateTime.now());

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request2.getCardNumber());

        // in Second call with dates

        Predicate<Transaction> datePredicate2 = AppUtils
                .datePredicate(request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().minusNanos(1).toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall1 = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(datePredicate2))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        Predicate<Transaction> datePredicate3 = AppUtils
                .datePredicate(request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        // after call result should be like this
        List<Transaction> mockUzCardTransactionsSecondCall2 = MockGenerator
                .getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(datePredicate3))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumber(),
                request2.getDateFrom().toLocalDate(),
                request.getDateFrom().minusNanos(1).toLocalDate()))
                .thenReturn(mockUzCardTransactionsSecondCall1);

        when(uzCardClient.getTransactionsBetweenDates(request2.getCardNumber(),
                request.getDateTo().plusNanos(1).toLocalDate(),
                request2.getDateTo().toLocalDate()))
                .thenReturn(mockUzCardTransactionsSecondCall2);

        // Second call
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request2);

        // Check for call client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // Check for not to call client
        Mockito.verify(uzCardClient, Mockito.times(0))
                .getTransactionsBetweenDates(request2.getCardNumber(), request2.getDateFrom().toLocalDate(), request2.getDateTo().toLocalDate());

        // Check for call to client 1
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request2.getDateFrom().toLocalDate(),
                        request.getDateFrom().minusNanos(1).toLocalDate());

        // Check for call to client 2
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateTo().plusNanos(1).toLocalDate(),
                        request2.getDateTo().toLocalDate());

        // Check for updated cache
        List<Transaction> fromCache2 = myCache
                .getAllBetween(request.getCardNumber(), request2.getDateFrom(), request2.getDateTo());


        // Check for correctly cached transactions

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request2.getDateFrom(),
                        request2.getDateTo());

        List<Transaction> cacheResultShouldBe = MockGenerator.getUzCardTransactions().stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(cacheResultShouldBe, fromCache2);


        // check for response
        Assertions.assertEquals(responseEntity.getStatusCode().value(), HttpStatus.OK.value());

        // response body
        List<TransactionDTO> body = responseEntity.getBody();

        Assertions.assertNotNull(body);

        // response body and client service data equality
        transactionsAndDTOsEquals(cacheResultShouldBe, body);

    }

    /**
     * Should get from client if another card cached
     * [1-10] in uzcard client
     * We called [2-6] with one card
     * And we call again this range with another card, and we get from client
     */
    @Test
    void shouldGetFromClientIfAnotherCardCached() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results
        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        // Second Call
        // Change card
        request.setCardNumber(uzCards.get(1));

        Assertions.assertTrue(myCache.isEmpty(request.getCardNumber(), request.getDateFrom(), request.getDateTo()));

        RangeDTO cacheRange2 = myCache.getCacheRange(request.getCardNumber());
        Assertions.assertNull(cacheRange2);

        setThisRequestAndCheckIt(request);

    }

    /**
     * Should get from uzcard client
     * But request is invalid fromDate > toDate
     */
    @Test
    void shouldGetFromClientByDateBetweenWithFromDateAfterToDate() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now();
        LocalDateTime toDate = LocalDateTime.now().minusDays(6);

        // Real results
        List<String> uzCards = MockGenerator.getUzCards();
        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

    }

    /**
     * Should update cached transactions
     */
    @Test
    void shouldUpdateCachedTransactions() {

        // check cache is empty
        Assertions.assertTrue(myCache.isEmpty());

        LocalDateTime fromDate = LocalDateTime.now().minusDays(6);
        LocalDateTime toDate = LocalDateTime.now();

        // Real results
        List<String> uzCards = MockGenerator.getUzCards();

        GetByDateRequest request = new GetByDateRequest(uzCards.get(0), fromDate, toDate);

        setThisRequestAndCheckIt(request);

        List<Transaction> fromCache1 = myCache.getAllBetween(request.getCardNumber(), request.getDateFrom(), request.getDateTo())
                .stream()
                .map(transaction -> Transaction.builder()
                        .id(transaction.getId())
                        .addedDate(transaction.getAddedDate())
                        .fromCard(transaction.getFromCard())
                        .toCard(transaction.getToCard())
                        .status(transaction.getStatus())
                        .amount(transaction.getAmount())
                        .build())
                .toList();

        String card = request.getCardNumber();
        RangeDTO cacheRange = myCache.getCacheRange(card);

        Predicate<Transaction> cardPredicate = AppUtils.cardPredicate(card);
        Predicate<Transaction> dateTimePredicate = AppUtils.dateTimePredicate(cacheRange.getFromDate(), cacheRange.getToDate());

        List<Transaction> mockTransactions = MockGenerator.getUzCardTransactions()
                .stream()
                .filter(cardPredicate.and(dateTimePredicate))
                .map(transaction -> Transaction.builder()
                        .amount(transaction.getAmount().add(BigDecimal.ONE))
                        .status(TransactionStatus.FAILED)
                        .fromCard(transaction.getFromCard())
                        .toCard(transaction.getToCard())
                        .addedDate(transaction.getAddedDate())
                        .build())
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();


        when(uzCardClient.getTransactionsBetweenDates(card, cacheRange.getFromDate().toLocalDate(), cacheRange.getToDate().toLocalDate()))
                .thenReturn(mockTransactions);

        transactionService.checkForCachedDataAndUpdate();

        Mockito.verify(uzCardClient, times(2))
                .getTransactionsBetweenDates(card, cacheRange.getFromDate().toLocalDate(), cacheRange.getToDate().toLocalDate());

        List<Transaction> transactions = myCache.getAllBetween(request.getCardNumber(), cacheRange.getFromDate(), cacheRange.getToDate());
        transactionAfterUpdateNotEquals(fromCache1, transactions);

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

    /**
     * Common method that call to client with this request and check it
     * 1 - cached correctly
     * 2 - cache range set correctly
     * 3 - response getting correctly
     *
     * @param request - CardNumber, From and To date
     */
    private void setThisRequestAndCheckIt(GetByDateRequest request) {

        Predicate<Transaction> cardPredicate = AppUtils
                .cardPredicate(request.getCardNumber());

        Predicate<Transaction> datePredicate = AppUtils
                .datePredicate(request.getDateFrom().toLocalDate(),
                        request.getDateTo().toLocalDate());

        Predicate<Transaction> dateTimePredicate = AppUtils
                .dateTimePredicate(request.getDateFrom(),
                        request.getDateTo());

        List<Transaction> mockUzCardTransactions = MockGenerator
                .getUzCardTransactions().stream()
                .filter(cardPredicate.and(datePredicate))
                .toList();

        // check for cache is empty
        Assertions.assertTrue(myCache.isEmpty(request.getCardNumber(), request.getDateFrom(), request.getDateTo()));

        when(uzCardClient.getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate()))
                .thenReturn(mockUzCardTransactions);

        // First call and save to cache
        ResponseEntity<List<TransactionDTO>> responseEntity = transactionService
                .getByDateBetween(request);

        // Check for call to client
        Mockito.verify(uzCardClient, Mockito.times(1))
                .getTransactionsBetweenDates(request.getCardNumber(), request.getDateFrom().toLocalDate(), request.getDateTo().toLocalDate());

        // check for response is cached
        Assertions.assertFalse(myCache
                .isEmpty(request.getCardNumber(), request.getDateFrom(), request.getDateTo()));

        // check for correctly cached
        List<Transaction> fromCache = myCache
                .getAllBetween(request.getCardNumber(), request.getDateFrom(), request.getDateTo());

        List<Transaction> mockUzCardTransactionsAfterCall = mockUzCardTransactions.stream()
                .filter(dateTimePredicate)
                .sorted(Comparator.comparing(Transaction::getAddedDate))
                .toList();

        // check for equality
        transactionsEquals(fromCache, mockUzCardTransactionsAfterCall);

        RangeDTO cacheRange = myCache.getCacheRange(request.getCardNumber());
        Assertions.assertNotNull(cacheRange);
        Assertions.assertEquals(request.getCardNumber(), cacheRange.getCardNumber());
        Assertions.assertEquals(request.getDateFrom(), cacheRange.getFromDate());
        Assertions.assertEquals(request.getDateTo(), cacheRange.getToDate());

        Assertions.assertNotNull(responseEntity);
        List<TransactionDTO> body = responseEntity.getBody();
        Assertions.assertNotNull(body);

        transactionsAndDTOsEquals(mockUzCardTransactionsAfterCall, body);

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
