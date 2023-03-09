package uz.md.synccache.clientService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.strategy.GetTransactionsStrategyContext;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
public class ClientServiceImpl implements ClientService {

    private final GetTransactionsStrategyContext getTransactionsStrategyContext;

    public ClientServiceImpl(GetTransactionsStrategyContext getTransactionsStrategyContext) {
        this.getTransactionsStrategyContext = getTransactionsStrategyContext;
    }

    @Override
    public List<Transaction> getTransactionsBetweenDays(String cardNumber, LocalDateTime dateFrom, LocalDateTime dateTo) {
       return getTransactionsStrategyContext
               .getTransactionsBetweenDays(cardNumber, dateFrom, dateTo);
    }
}
