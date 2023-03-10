package uz.md.synccache.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.dtos.RangeDTO;
import uz.md.synccache.entity.Range;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.mapper.RangeMapper;
import uz.md.synccache.repository.CardView;
import uz.md.synccache.repository.RangeRepository;
import uz.md.synccache.repository.TransactionRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
@Transactional
@Slf4j
@RequiredArgsConstructor
public class MyCache {

    private final RangeRepository rangeRepository;

    private final TransactionRepository transactionRepository;
    private final RangeMapper rangeMapper;

    public void put(Transaction value) {
        log.info("put to cache " + value);

        if (value == null
                || value.getFromCard() == null
                || value.getToCard() == null
                || value.getAddedDate() == null
                || value.getAmount() == null)
            return;

        if (transactionRepository
                .existsByFromCardAndToCardAndAddedDate(
                        value.getFromCard(),
                        value.getToCard(),
                        value.getAddedDate())) {
            transactionRepository
                    .findByFromCardAndToCardAndAddedDate(value.getFromCard(), value.getToCard(), value.getAddedDate())
                    .ifPresent(transaction -> value.setId(transaction.getId()));
        }
        transactionRepository.save(value);
    }

    public Optional<Transaction> get(Long key) {
        log.info("get from cache with key: " + key);
        return transactionRepository.findById(key);
    }

    public void putAll(List<Transaction> list) {
        log.info(" put all to cache " + list);
        for (Transaction transaction : list) {
            put(transaction);
        }
    }

    public List<Transaction> getAllBetween(String cardNumber, LocalDateTime dateFrom, LocalDateTime dateTo) {
        log.info(" get between date " + dateFrom + " and " + dateTo);

        if (cardNumber == null || dateFrom == null || dateTo == null)
            throw new BadRequestException("Request is invalid");
        return transactionRepository
                .findAllByCardAndAddedDateBetween(cardNumber, dateFrom, dateTo);
    }

    public void invalidateAll() {
        log.info("invalidating all");
        transactionRepository.deleteAll();
    }

    public Optional<Transaction> get(LocalDateTime addedDate) {
        return transactionRepository
                .findByAddedDate(addedDate);
    }

    public List<Transaction> getAll() {
        return transactionRepository.findAll();
    }

    public boolean containsKey(Long key) {
        return transactionRepository.existsById(key);
    }

    public boolean isEmpty() {
        return transactionRepository.count() == 0;
    }

    public boolean isEmpty(String cardNumber, LocalDateTime dateFrom, LocalDateTime dateTo) {

        if (cardNumber == null || dateFrom == null || dateTo == null)
            throw new BadRequestException("Request is invalid");

        return !rangeRepository.existsByCardNumber(cardNumber);
    }

    public Set<String> getCards() {
        List<CardView> allCards = rangeRepository.findAllCards();
        Set<String> cards = new HashSet<>();
        for (CardView transactionCard : allCards) {
            cards.add(transactionCard.getCard());
        }
        return cards;
    }

    public RangeDTO getCacheRange(String cardNumber) {
        if (cardNumber == null)
            throw new BadRequestException("Invalid card number");
        Optional<Range> optional = rangeRepository
                .findByCardNumber(cardNumber);
        if (optional.isPresent()) {
            Range range = optional.get();
            return rangeMapper.toDTO(range);
        }
        return null;
    }

    public void setCachedRange(String cardNumber, LocalDateTime from, LocalDateTime to) {
        if (cardNumber == null || from == null || to == null)
            throw new BadRequestException("Invalid request");

        if (rangeRepository.existsByCardNumber(cardNumber)) {
            rangeRepository.deleteByCardNumber(cardNumber);
        }

        rangeRepository.save(Range.builder()
                .cardNumber(cardNumber)
                .fromDate(from)
                .toDate(to)
                .build());
    }

    public void deleteAllRanges() {
        rangeRepository.deleteAll();
    }

    public boolean existsCacheRangeByCardNumber(String cardNumber) {
        return rangeRepository.existsByCardNumber(cardNumber);
    }
}
