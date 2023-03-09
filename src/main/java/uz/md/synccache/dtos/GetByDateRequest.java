package uz.md.synccache.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class GetByDateRequest {
    @NotEmpty(message = "card number is required")
    private String cardNumber;
    @NotNull(message = " FromDate can not be null")
    private LocalDateTime dateFrom;
    @NotNull(message = " ToDate can not be null")
    private LocalDateTime dateTo;

}
