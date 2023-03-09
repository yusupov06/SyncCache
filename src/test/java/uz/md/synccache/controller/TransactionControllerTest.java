package uz.md.synccache.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import uz.md.synccache.repository.TransactionRepository;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class TransactionControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private TransactionRepository transactionRepository;
//
//    @Test
//    void shouldGetById() throws Exception {
//        Transaction transaction = transactionRepository
//                .findById(15L)
//                .orElseThrow(() -> new NotFoundException("Transaction not found"));
//        mvc.perform(MockMvcRequestBuilders
//                        .get("/api/v1/transaction/15"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.id").value(transaction.getId()))
//                .andExpect(jsonPath("$.amount").value(transaction.getAmount()))
//                .andExpect(jsonPath("$.fromCard").value(transaction.getFromCard()))
//                .andExpect(jsonPath("$.toCard").value(transaction.getToCard()))
//        ;
//    }


}
