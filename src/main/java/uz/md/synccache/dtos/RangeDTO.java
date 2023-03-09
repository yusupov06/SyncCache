package uz.md.synccache.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RangeDTO {
    private String cardNumber;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
