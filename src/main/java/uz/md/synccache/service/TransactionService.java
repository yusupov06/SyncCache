package uz.md.synccache.service;

import org.springframework.http.ResponseEntity;
import uz.md.synccache.dtos.GetByDateRequest;
import uz.md.synccache.dtos.TransactionDTO;

import java.util.List;
import java.util.Map;

public interface TransactionService {

    ResponseEntity<Map<String, List<TransactionDTO>>> getByDateBetween(GetByDateRequest request);

    void checkForCachedDataAndUpdate();


}
