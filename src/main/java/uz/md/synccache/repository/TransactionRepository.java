package uz.md.synccache.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.md.synccache.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("FROM Transaction where fromCard = :card or toCard = :card and addedDate>= :fromDate and addedDate <= :toDate order by addedDate")
    List<Transaction> findAllByCardAndAddedDateBetween(String card,
                                                       LocalDateTime fromDate,
                                                       LocalDateTime toDate);

    Optional<Transaction> findByAddedDate(LocalDateTime addedDate);

    Boolean existsByFromCardOrToCardAndAddedDateBetween(String fromCard, String toCard, LocalDateTime addedDate, LocalDateTime addedDate2);

    @Query("select max(addedDate) from Transaction")
    CachedRangeView findCachedRangeWithCard(String cardNumber);

    boolean existsByFromCardAndToCardAndAddedDate(String fromCard, String toCard, LocalDateTime addedDate);

    void deleteByFromCardAndToCardAndAddedDate(String fromCard, String toCard, LocalDateTime addedDate);

    Optional<Transaction> findByFromCardAndToCardAndAddedDate(String fromCard, String toCard, LocalDateTime addedDate);

}
