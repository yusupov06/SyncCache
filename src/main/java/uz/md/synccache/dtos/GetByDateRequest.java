package uz.md.synccache.dtos;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class GetByDateRequest {
    @NotNull(message = "card numbers is required")
    private List<String> cardNumbers;
    @NotNull(message = " FromDate can not be null")
    private LocalDateTime dateFrom;
    @NotNull(message = " ToDate can not be null")
    private LocalDateTime dateTo;

}
