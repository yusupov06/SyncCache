package uz.md.synccache.mapper;

import org.mapstruct.Mapper;
import uz.md.synccache.dtos.TransactionDTO;
import uz.md.synccache.entity.Transaction;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionDTO toDTO(Transaction transaction);

    List<TransactionDTO> toDTO(List<Transaction> currencies);
}
