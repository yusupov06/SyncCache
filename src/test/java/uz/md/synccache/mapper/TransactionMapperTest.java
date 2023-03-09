package uz.md.synccache.mapper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uz.md.synccache.dtos.TransactionDTO;
import uz.md.synccache.entity.Transaction;
import uz.md.synccache.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest
class TransactionMapperTest {

    @MockBean
    private TransactionMapper transactionMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testToDTO() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("10.00"));
        transaction.setFromCard("1234567890");
        transaction.setToCard("0987654321");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setAddedDate(LocalDateTime.now());

        TransactionDTO expectedDTO = new TransactionDTO();
        expectedDTO.setId(1L);
        expectedDTO.setAmount(new BigDecimal("10.00"));
        expectedDTO.setFromCard("1234567890");
        expectedDTO.setToCard("0987654321");
        expectedDTO.setStatus(TransactionStatus.SUCCESS);
        expectedDTO.setAddedDate(LocalDateTime.now());

        when(transactionMapper.toDTO(transaction)).thenReturn(expectedDTO);

        TransactionDTO actualDTO = transactionMapper.toDTO(transaction);

        assertEquals(expectedDTO, actualDTO);
    }

    @Test
    public void testToDTOWithNullValues() {
        // given
        Transaction transaction = new Transaction();
        transaction.setId(null);
        transaction.setAmount(null);
        transaction.setFromCard(null);
        transaction.setToCard(null);
        transaction.setAddedDate(null);
        transaction.setStatus(null);

        TransactionDTO expectedDTO = new TransactionDTO();
        expectedDTO.setId(null);
        expectedDTO.setAmount(null);
        expectedDTO.setFromCard(null);
        expectedDTO.setToCard(null);
        expectedDTO.setStatus(null);
        expectedDTO.setAddedDate(null);

        when(transactionMapper.toDTO(transaction)).thenReturn(expectedDTO);

        // when
        TransactionDTO dto = transactionMapper.toDTO(transaction);

        // then
        Assertions.assertThat(dto.getId()).isNull();
        Assertions.assertThat(dto.getAmount()).isNull();
        Assertions.assertThat(dto.getFromCard()).isNull();
        Assertions.assertThat(dto.getToCard()).isNull();
        Assertions.assertThat(dto.getAddedDate()).isNull();
        Assertions.assertThat(dto.getStatus()).isNull();
    }

    @Test
    void testToDTOList() {

        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("10.00"));
        transaction.setFromCard("1234567890");
        transaction.setToCard("0987654321");
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setAddedDate(LocalDateTime.now());

        List<Transaction> transactions = List.of(transaction);

        TransactionDTO expectedDTO = new TransactionDTO();
        expectedDTO.setId(1L);
        expectedDTO.setAmount(new BigDecimal("10.00"));
        expectedDTO.setFromCard("1234567890");
        expectedDTO.setToCard("0987654321");
        expectedDTO.setStatus(TransactionStatus.SUCCESS);
        expectedDTO.setAddedDate(LocalDateTime.now());

        when(transactionMapper.toDTO(transactions)).thenReturn(List.of(expectedDTO));

        List<TransactionDTO> transactionDTOs = transactionMapper.toDTO(transactions);
        assertEquals(transactions.size(), transactionDTOs.size());
        TransactionDTO transactionDTO = transactionDTOs.get(0);
        assertEquals(transaction.getId(), transactionDTO.getId());
        assertEquals(transaction.getAmount(), transactionDTO.getAmount());
        assertEquals(transaction.getFromCard(), transactionDTO.getFromCard());
        assertEquals(transaction.getToCard(), transactionDTO.getToCard());
        assertEquals(transaction.getAddedDate(), transactionDTO.getAddedDate());
        assertEquals(transaction.getStatus(), transactionDTO.getStatus());
    }


}
