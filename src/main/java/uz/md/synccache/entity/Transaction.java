package uz.md.synccache.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"from_card", "to_card", "added_date"})})
@Builder
@ToString
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, name = "from_card")
    private String fromCard; // transaction from

    @Column(nullable = false, name = "to_card")
    private String toCard; // transaction to

    private TransactionStatus status; // transaction status

    @Column(nullable = false, name = "added_date")
    private LocalDateTime addedDate = LocalDateTime.now(); // date

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transaction)) {
            return false;
        }
        return getId() != null && getId().equals(((Transaction) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
