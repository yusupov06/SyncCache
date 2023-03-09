package uz.md.synccache.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table
@Getter
@Setter
@Entity
public class Range {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String cardNumber;

    private LocalDateTime fromDate;

    private LocalDateTime toDate;

}
