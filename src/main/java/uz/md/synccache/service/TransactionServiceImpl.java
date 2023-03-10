package uz.md.synccache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.cache.MyCache;
import uz.md.synccache.dtos.GetByDateRequest;
import uz.md.synccache.dtos.RangeDTO;
import uz.md.synccache.dtos.TransactionDTO;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.mapper.TransactionMapper;
import uz.md.synccache.strategy.GetTransactionsComposite;
import uz.md.synccache.utils.AppUtils;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final MyCache myCache;
    private final GetTransactionsComposite transactionsComposite;

    @Override
    public ResponseEntity<Map<String, List<TransactionDTO>>> getByDateBetween(GetByDateRequest request) {

        log.info("Getting by date between ");

        // Request validation
        if (request == null
                || request.getDateTo() == null
                || request.getDateFrom() == null
                || request.getCardNumbers() == null) {
            throw new BadRequestException("Request cannot be null");
        }

        // Swap if fromDate > toDate
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            log.info("Request dates swap");
            LocalDateTime dateFrom = request.getDateFrom();
            request.setDateFrom(request.getDateTo());
            request.setDateTo(dateFrom);
        }

        Map<String, List<TransactionDTO>> response = new HashMap<>();

        List<String> cardsThatNotCached = new ArrayList<>();

        for (String cardNumber : request.getCardNumbers()) {
            if (myCache.existsCacheRangeByCardNumber(cardNumber)) {
                List<Transaction> fromCache = getFromCache(cardNumber, request.getDateFrom(), request.getDateTo());
                if (fromCache != null)
                    response.put(cardNumber, transactionMapper.toDTO(fromCache));
            } else
                cardsThatNotCached.add(cardNumber);
        }

        Map<String, List<Transaction>> transactionsMap = transactionsComposite
                .getTransactionsBetweenDays(cardsThatNotCached, request.getDateFrom(), request.getDateTo());

        transactionsMap.forEach((cardNum, transactions) -> {
            if (transactions != null) {
                if (transactions.size() != 0) {
                    myCache.setCachedRange(cardNum, request.getDateFrom(), request.getDateTo());
                    myCache.putAll(transactions);
                }
                response.put(cardNum, transactionMapper.toDTO(transactions));
            }
        });

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private List<Transaction> getFromCache(String cardNumber, LocalDateTime fromDate, LocalDateTime toDate) {

        log.info("Getting from cache and client ");

        RangeDTO cachedRange = myCache.getCacheRange(cardNumber);

        // If range is not null, and transactions existed in our cache with this request
        List<Transaction> fromCache = new ArrayList<>(myCache
                .getAllBetween(cardNumber, fromDate, toDate));

        // if transactions with this request is empty
        // We have to get from client
        if (fromCache.isEmpty()) {

            log.info("Getting from client with request " + cardNumber + " " + fromDate + "" + toDate);

            LocalDateTime from = fromDate;
            LocalDateTime to = toDate;

            // we have to set fromDate
            // if we after cached transactions we have to know fromDate
            if (cachedRange.getToDate() != null
                    && cachedRange.getToDate().isBefore(from)) {
                from = cachedRange.getToDate();
            }

            // we have to set toDate
            // if we after cached transactions we have to know toDate
            if (cachedRange.getFromDate() != null
                    && cachedRange.getFromDate().isAfter(to)) {
                to = cachedRange.getFromDate();
            }

            // Get transactions from client
            Map<String, List<Transaction>> transactionsMap = transactionsComposite
                    .getTransactionsBetweenDays(List.of(cardNumber), from, to);

            if (transactionsMap == null || transactionsMap.containsKey(cardNumber))
                return new ArrayList<>();

            List<Transaction> transactions = transactionsMap.get(cardNumber);

            if (transactions != null) {
                // save transactions to cache
                myCache.putAll(transactions);
                // set new range to this card
                myCache.setCachedRange(cardNumber, from, to);

                return transactions;
            }
            return new ArrayList<>();
        }

        // If fromCache is not empty we get not existed transactions from client,
        // or we return this fromCache

        List<Transaction> transactions = new ArrayList<>();

        // This is range of transactions with this card in our cache
        LocalDateTime cachedFrom = cachedRange.getFromDate();
        LocalDateTime cachedTo = cachedRange.getToDate();
        LocalDateTime rangeFrom = cachedFrom;
        LocalDateTime rangeTo = cachedTo;

        if (fromDate.isBefore(cachedFrom)
                && toDate.isAfter(cachedTo)) {

            // If our request is getting [1-10] range but our cache range is [4-7]
            // we get from client with two calls [1-3] and [8-10]

            Map<String, List<Transaction>> fromClient1 = transactionsComposite
                    .getTransactionsBetweenDays(List.of(cardNumber), fromDate, cachedFrom.minusNanos(1));
            if (fromClient1 != null
                    && fromClient1.size() != 0
                    && fromClient1.containsKey(cardNumber)) {
                rangeFrom = fromDate;
                transactions.addAll(fromClient1.get(cardNumber));
            }

            Map<String, List<Transaction>> fromClient2 = transactionsComposite
                    .getTransactionsBetweenDays(List.of(cardNumber), cachedTo.plusNanos(1), toDate);

            if (fromClient2 != null
                    && fromClient2.size() != 0
                    && fromClient2.containsKey(cardNumber)) {
                rangeTo = toDate;
                transactions.addAll(fromClient2.get(cardNumber));
            }

        } else if (fromDate.isBefore(cachedFrom)) {

            // If our request is getting [1-5] range but our cache range is [4-5] or [4-6]
            // we get from client with a call [1-3]

            Map<String, List<Transaction>> fromClient = transactionsComposite
                    .getTransactionsBetweenDays(List.of(cardNumber), fromDate, cachedFrom.minusNanos(1));
            if (fromClient != null
                    && fromClient.size() != 0
                    && fromClient.containsKey(cardNumber)) {
                rangeFrom = fromDate;
                transactions.addAll(fromClient.get(cardNumber));
            }

        } else if (toDate.isAfter(cachedTo)) {

            // If our request is getting [2-8] range but our cache range is [1-5] or [2-5]
            // we get from client with a call [6-8]

            Map<String, List<Transaction>> fromClient = transactionsComposite
                    .getTransactionsBetweenDays(List.of(cardNumber), cachedTo.plusNanos(1), toDate);
            if (fromClient != null
                    && fromClient.size() != 0
                    && fromClient.containsKey(cardNumber)) {
                rangeTo = toDate;
                transactions.addAll(fromClient.get(cardNumber));
            }
        }

        transactions.addAll(fromCache);

        transactions.sort(Comparator.comparing(Transaction::getAddedDate));

        myCache.setCachedRange(cardNumber, rangeFrom, rangeTo);
        myCache.putAll(transactions);

        return transactions.stream()
                .filter(AppUtils.dateTimePredicate(fromDate, toDate))
                .toList();
    }


    @Override
    public void checkForCachedDataAndUpdate() {

        log.info("Checking for cached data and update");

        // if cache is empty we don't update
        if (myCache.isEmpty()) {
            log.info("No data in cache");
            return;
        }

        // Get cards that we cached
        Set<String> cards = myCache.getCards();

        // and update cache
        for (String card : cards) {
            RangeDTO cacheRange = myCache.getCacheRange(card);
            if (cacheRange != null && cacheRange.getFromDate() != null && cacheRange.getToDate() != null) {
                Map<String, List<Transaction>> listMap = transactionsComposite
                        .getTransactionsBetweenDays(List.of(card), cacheRange.getFromDate(), cacheRange.getToDate());
                if (listMap != null && !listMap.isEmpty())
                    myCache.putAll(listMap.get(card));
            }
        }

    }

}
