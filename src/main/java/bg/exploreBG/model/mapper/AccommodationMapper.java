package bg.exploreBG.model.mapper;

import bg.exploreBG.model.dto.accommodation.AccommodationDetailsDto;
import bg.exploreBG.model.dto.accommodation.validate.AccommodationCreateDto;
import bg.exploreBG.model.entity.AccommodationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AccommodationMapper {

    @Mapping(source = "access.value", target = "access")
    @Mapping(source = "type.value", target = "type")
    AccommodationDetailsDto accommodationEntityToAccommodationDetailsDto(AccommodationEntity accommodationEntity);

    AccommodationEntity accommodationCreateDtoToAccommodationEntity (AccommodationCreateDto accommodationCreateDto);
}
