package uz.md.synccache.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.md.synccache.entity.Range;

import java.util.List;
import java.util.Optional;

@Repository
public interface RangeRepository extends JpaRepository<Range, Long> {

    Optional<Range> findByCardNumber(String cardNumber);

    boolean existsByCardNumber(String cardNumber);

    void deleteByCardNumber(String cardNumber);

    @Query("select r.cardNumber as card from Range r")
    List<CardView> findAllCards();
}
