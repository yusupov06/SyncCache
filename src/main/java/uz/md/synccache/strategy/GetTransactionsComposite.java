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

        GetTransactionsStrategy strategy;

        if (!strategyMap.containsKey(cardPrefix)) {

            // Get all the bean definition names in the context
            String[] beanNames = context.getBeanDefinitionNames();

            // Loop all beans and add to map
            for (String beanName : beanNames) {

                Object bean = context.getBean(beanName);
                if (bean instanceof GetTransactionsStrategy) {
                    System.out.println(" ########## bean = " + bean);
                    strategy = (GetTransactionsStrategy) bean;
                    if (!strategy.getClass().getSimpleName().equals(GetTransactionsComposite.class.getSimpleName())
                            && !strategyMap.containsValue(strategy)) {
                        System.out.println("Added ############ bean = " + bean);
                        addStrategy(strategy);
                    }
                }
            }

            if (!strategyMap.containsKey(cardPrefix))
                throw new NotFoundException("Not found strategy");
        }

        return strategyMap.get(cardPrefix);
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
