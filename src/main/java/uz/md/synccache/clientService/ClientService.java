package uz.md.synccache.clientService;


import uz.md.synccache.entity.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public interface ClientService {

    List<Transaction> getTransactionsBetweenDays(String cardNumber, LocalDateTime dateFrom, LocalDateTime dateTo);

}
