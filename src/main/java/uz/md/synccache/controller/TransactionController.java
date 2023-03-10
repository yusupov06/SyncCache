package uz.md.synccache.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uz.md.synccache.dtos.GetByDateRequest;
import uz.md.synccache.dtos.TransactionDTO;
import uz.md.synccache.service.TransactionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transaction/date-between")
    public ResponseEntity<Map<String, List<TransactionDTO>>> getByDate(@RequestBody @Valid GetByDateRequest request) {
        return transactionService.getByDateBetween(request);
    }

}
