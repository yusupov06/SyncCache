package uz.md.synccache.component;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uz.md.synccache.utils.MockGenerator;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile(value = {"dev", "test"})
public class DataLoader implements CommandLineRunner {

    @Override
    public void run(String... args) {
        initData();
    }

    private void initData() {
        MockGenerator.setUzCards(8);
        MockGenerator.setUzCardTransactions(MockGenerator
                .generateMockUzCardTransactions(200));
    }
}
