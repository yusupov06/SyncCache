package uz.md.synccache.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.stereotype.Component;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.exceptions.BadRequestException;
import uz.md.synccache.exceptions.NotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
//@ComponentScan(basePackages = {"uz.md.synccache.strategy"})
@ComponentScan(basePackageClasses = {GetTransactionsStrategy.class})
public class GetTransactionsStrategyContext {

    private final ApplicationContext context;

    private Map<String, GetTransactionsStrategy> beans = new HashMap<>();

    public List<Transaction> getTransactionsBetweenDays(String cardNumber, LocalDateTime dateFrom,
                                                        LocalDateTime dateTo) {

        if (cardNumber == null || dateFrom == null || dateTo == null)
            throw new BadRequestException("Bad request");

        String cardPrefix = cardNumber.substring(0, 4);

        GetTransactionsStrategy strategy;

        if (!beans.containsKey(cardPrefix)) {

//            ApplicationContext context = new ClassPathXmlApplicationContext("uz.md.synccache.strategy");

            // Get all the bean definition names in the context
            String[] beanNames = context.getBeanDefinitionNames();

            // Print the names of all the beans in the context
            for (String beanName : beanNames) {

                System.out.println(" ################# beanName = " + beanName);

                Object bean = context.getBean(beanName);
                if (bean instanceof GetTransactionsStrategy) {
                    strategy = (GetTransactionsStrategy) bean;
                    if (!beans.containsValue(strategy))
                        beans.put(strategy.getCardPrefix(), strategy);
                }
            }

            if (!beans.containsKey(cardPrefix))
                throw new NotFoundException("Not found strategy");
        }
        strategy = beans.get(cardPrefix);
        return strategy
                .getTransactionsBetweenDays(cardNumber, dateFrom, dateTo);

    }

}
