package uz.md.synccache.mapper;

import org.mapstruct.Mapper;
import uz.md.synccache.dtos.RangeDTO;
import uz.md.synccache.entity.Range;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RangeMapper {

    RangeDTO toDTO(Range range);

    List<RangeDTO> toDTO(List<Range> ranges);
}
