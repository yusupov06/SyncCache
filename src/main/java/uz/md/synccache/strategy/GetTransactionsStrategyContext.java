package uz.md.synccache.strategy;

import org.springframework.stereotype.Component;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class GetTransactionsStrategyContext {

    private final UzCardGetTransactionStrategy uzCardGetTransactionStrategy;
    private final HumoGetTransactionsStrategy humoGetTransactionsStrategy;

    public GetTransactionsStrategyContext(UzCardGetTransactionStrategy uzCardGetTransactionStrategy,
                                          HumoGetTransactionsStrategy humoGetTransactionsStrategy) {
        this.uzCardGetTransactionStrategy = uzCardGetTransactionStrategy;
        this.humoGetTransactionsStrategy = humoGetTransactionsStrategy;
    }

    public List<Transaction> getTransactionsBetweenDays(String cardNumber, LocalDateTime dateFrom,
                                                        LocalDateTime dateTo) {

        if (cardNumber == null || dateFrom == null || dateTo == null)
            throw new BadRequestException("Bad request");

        GetTransactionsStrategy strategy;

        strategy = switch (cardNumber.substring(0,4)) {
            case "8600" -> uzCardGetTransactionStrategy;
            case "9860" -> humoGetTransactionsStrategy;
            default -> null;
        };

        if (strategy == null)
            throw new NotFoundException("Strategy not found");

        return strategy
                .getTransactionsBetweenDays(cardNumber, dateFrom, dateTo);

    }

}
