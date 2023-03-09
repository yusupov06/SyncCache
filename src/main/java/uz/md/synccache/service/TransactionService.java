package uz.md.synccache.service;

import org.springframework.http.ResponseEntity;
import uz.md.synccache.dtos.GetByDateRequest;
import uz.md.synccache.dtos.TransactionDTO;

import java.util.List;

public interface TransactionService {

    ResponseEntity<List<TransactionDTO>> getByDateBetween(GetByDateRequest request);

    void checkForCachedDataAndUpdate();


}
