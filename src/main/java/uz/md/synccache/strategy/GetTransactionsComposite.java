package uz.md.synccache.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.exceptions.NotFoundException;
import uz.md.synccache.utils.AppUtils;

import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class GetTransactionsComposite implements GetTransactionsStrategy {

    private final ApplicationContext context;

    private final Map<String, GetTransactionsStrategy> strategyMap = new HashMap<>();

    @Override
    public String getCardPrefix() {
        return strategyMap.values().toString();
    }

    @Override
    public Map<String, List<Transaction>> getTransactionsBetweenDays(List<String> cards, LocalDateTime dateFrom, LocalDateTime dateTo) {
        if (cards == null || dateFrom == null || dateTo == null)
            throw new BadRequestException("Bad request");

        Map<String, List<Transaction>> response = new HashMap<>();
        Map<String, List<String>> cardsGroup = AppUtils.getCardsGroup(cards);

        cardsGroup.forEach((prefix, list) -> {
            GetTransactionsStrategy strategy = getStrategy(prefix);
            Map<String, List<Transaction>> transactions = strategy
                    .getTransactionsBetweenDays(list, dateFrom, dateTo);
            if (transactions != null)
                response.putAll(transactions);
        });

        return response;
    }

    private GetTransactionsStrategy getStrategy(String cardPrefix) {

        GetTransactionsStrategy strategy = strategyMap.get(cardPrefix);

        if (strategy == null) {

            // Get all the beans in the context that are GetTransactionsStrategy implementation
            Map<String, GetTransactionsStrategy> beans = context.getBeansOfType(GetTransactionsStrategy.class);

            // find strategy from context that prefix is cardPrefix
            strategy = beans.values()
                    .stream()
                    .filter(s -> s.getCardPrefix().equalsIgnoreCase(cardPrefix))
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("No such card prefixed strategy"));
        }

        return strategy;
    }

    public void addStrategy(GetTransactionsStrategy strategy) {
        strategyMap.put(strategy.getCardPrefix(), strategy);
    }

    public void removeStrategy(GetTransactionsStrategy strategy) {
        strategyMap.remove(strategy.getCardPrefix());
    }

    public void clearAll() {
        strategyMap.clear();
    }


}
