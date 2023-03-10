//package uz.md.synccache.strategy;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.support.BeanDefinitionRegistry;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
//import org.springframework.context.support.ClassPathXmlApplicationContext;
//import org.springframework.stereotype.Component;
//import uz.md.synccache.entity.Transaction;
//import uz.md.synccache.exceptions.BadRequestException;
//import uz.md.synccache.exceptions.NotFoundException;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Component
//@RequiredArgsConstructor
//public class GetTransactionsStrategyContext {
//
//    private final ApplicationContext context;
//
//    private final Map<String, GetTransactionsStrategy> strategyMap = new HashMap<>();
//
//
//    public List<Transaction> getTransactionsBetweenDays(String cardNumber, LocalDateTime dateFrom,
//                                                        LocalDateTime dateTo) {
//
//        if (cardNumber == null || dateFrom == null || dateTo == null)
//            throw new BadRequestException("Bad request");
//
//        String cardPrefix = cardNumber.substring(0, 4);
//
//        GetTransactionsStrategy strategy = getStrategy(cardPrefix);
//        return strategy
//                .getTransactionsBetweenDays(cardNumber, dateFrom, dateTo);
//    }
//
//    private GetTransactionsStrategy getStrategy(String cardPrefix) {
//
//        GetTransactionsStrategy strategy;
//
//        if (!strategyMap.containsKey(cardPrefix)) {
//
//            // Get all the bean definition names in the context
//            String[] beanNames = context.getBeanDefinitionNames();
//
//            // Loop all beans and add to map
//            for (String beanName : beanNames) {
//
//                 Object bean = context.getBean(beanName);
//                if (bean instanceof GetTransactionsStrategy) {
//                    strategy = (GetTransactionsStrategy) bean;
//                    if (!strategyMap.containsValue(strategy))
//                        strategyMap.put(strategy.getCardPrefix(), strategy);
//                }
//            }
//
//            if (!strategyMap.containsKey(cardPrefix))
//                throw new NotFoundException("Not found strategy");
//        }
//
//        return strategyMap.get(cardPrefix);
//    }
//
//}
