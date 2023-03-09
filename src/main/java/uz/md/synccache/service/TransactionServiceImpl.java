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
import uz.md.synccache.strategy.GetTransactionsStrategyContext;
import uz.md.synccache.utils.AppUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionMapper transactionMapper;
    private final MyCache myCache;
    private final GetTransactionsStrategyContext getTransactionsStrategyContext;

    @Override
    public ResponseEntity<List<TransactionDTO>> getByDateBetween(GetByDateRequest request) {

        log.info("Getting by date between ");

        // Request validation
        if (request == null
                || request.getDateTo() == null
                || request.getDateFrom() == null) {
            throw new BadRequestException("Request cannot be null");
        }

        // Swap if fromDate > toDate
        if (request.getDateFrom().isAfter(request.getDateTo())) {
            log.info("Request dates swap");
            LocalDateTime dateFrom = request.getDateFrom();
            request.setDateFrom(request.getDateTo());
            request.setDateTo(dateFrom);
        }

        // Get cached range of card
        RangeDTO cachedRange = myCache.getCacheRange(request.getCardNumber());

        // If no range with this card this is the first call
        if (cachedRange == null
                || cachedRange.getFromDate() == null
                || cachedRange.getToDate() == null) {

            List<Transaction> fromClient = getTransactionsStrategyContext
                    .getTransactionsBetweenDays(request.getCardNumber(), request.getDateFrom(), request.getDateTo());

            if (fromClient == null)
                fromClient = new ArrayList<>();
            myCache.setCachedRange(request.getCardNumber(),
                    request.getDateFrom(), request.getDateTo());
            myCache.putAll(fromClient);

            return new ResponseEntity<>(transactionMapper
                    .toDTO(fromClient), HttpStatus.OK);
        }

        // If range is not null, and transactions existed in our cache with this request
        List<Transaction> fromCache = new ArrayList<>(myCache
                .getAllBetween(request.getCardNumber(), request.getDateFrom(), request.getDateTo()));

        // if transactions with this request is empty
        // We have to get from client
        if (fromCache.isEmpty()) {

            log.info("Getting from client with request " + request);

            LocalDateTime from = request.getDateFrom();
            LocalDateTime to = request.getDateTo();

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
            List<Transaction> allByDateBetween = getTransactionsStrategyContext
                    .getTransactionsBetweenDays(request.getCardNumber(), from, to);

            if (allByDateBetween == null)
                allByDateBetween = new ArrayList<>();

            // save transactions to cache
            myCache.putAll(allByDateBetween);
            // set new range to this card
            myCache.setCachedRange(request.getCardNumber(), from, to);

            return new ResponseEntity<>(transactionMapper
                    .toDTO(allByDateBetween), HttpStatus.OK);

        }

        // If fromCache is not empty we get not existed transactions from client,
        // or we return this fromCache

        List<Transaction> transactions = new ArrayList<>();

        // This is range of transactions with this card in our cache
        LocalDateTime cachedFrom = cachedRange.getFromDate();
        LocalDateTime cachedTo = cachedRange.getToDate();
        LocalDateTime rangeFrom = cachedFrom;
        LocalDateTime rangeTo = cachedTo;

        if (request.getDateFrom().isBefore(cachedFrom)
                && request.getDateTo().isAfter(cachedTo)) {

            // If our request is getting [1-10] range but our cache range is [4-7]
            // we get from client with two calls [1-3] and [8-10]

            List<Transaction> fromClient1 = getTransactionsStrategyContext
                    .getTransactionsBetweenDays(request.getCardNumber(), request.getDateFrom(), cachedFrom.minusNanos(1));

            if (fromClient1 != null && fromClient1.size() != 0) {
                rangeFrom = request.getDateFrom();
                transactions.addAll(fromClient1);
            }

            transactions.addAll(fromCache);

            List<Transaction> fromClient2 = getTransactionsStrategyContext
                    .getTransactionsBetweenDays(request.getCardNumber(), cachedTo.plusNanos(1), request.getDateTo());
            if (fromClient2 != null && fromClient2.size() != 0) {
                rangeTo = request.getDateTo();
                transactions.addAll(fromClient2);
            }
        } else if (request.getDateFrom().isBefore(cachedFrom)) {

            // If our request is getting [1-5] range but our cache range is [4-5] or [4-6]
            // we get from client with a call [1-3]

            transactions.addAll(fromCache);
            List<Transaction> fromClient = getTransactionsStrategyContext
                    .getTransactionsBetweenDays(request.getCardNumber(), request.getDateFrom(), cachedFrom.minusNanos(1));
            if (fromClient != null && fromClient.size() != 0) {
                rangeFrom = request.getDateFrom();
                transactions.addAll(fromClient);
            }

        } else if (request.getDateTo().isAfter(cachedTo)) {

            // If our request is getting [2-8] range but our cache range is [1-5] or [2-5]
            // we get from client with a call [6-8]

            transactions.addAll(fromCache);
            List<Transaction> fromClient = getTransactionsStrategyContext
                    .getTransactionsBetweenDays(request.getCardNumber(), cachedTo.plusNanos(1), request.getDateTo());
            if (fromClient != null && fromClient.size() != 0) {
                rangeTo = request.getDateTo();
                transactions.addAll(fromClient);
            }
        } else {

            // If our request is getting [2-4] range and our cache range is [2-4]
            // we return this
            transactions.addAll(fromCache);
        }

        transactions.sort(Comparator.comparing(Transaction::getAddedDate));

        myCache.setCachedRange(request.getCardNumber(), rangeFrom, rangeTo);
        myCache.putAll(transactions);

        List<Transaction> res = transactions.stream()
                .filter(AppUtils.dateTimePredicate(request.getDateFrom(), request.getDateTo()))
                .toList();

        return new ResponseEntity<>(transactionMapper
                .toDTO(res), HttpStatus.OK);

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
                List<Transaction> list = getTransactionsStrategyContext
                        .getTransactionsBetweenDays(card, cacheRange.getFromDate(), cacheRange.getToDate());
                if (list != null && !list.isEmpty())
                    myCache.putAll(list);
            }
        }

    }

}
