package bg.exploreBG.model.mapper;

import bg.exploreBG.model.dto.hikingTrail.HikingTrailBasicDto;
import bg.exploreBG.model.dto.hikingTrail.HikingTrailDetailsDto;
import bg.exploreBG.model.entity.HikingTrailEntity;
import bg.exploreBG.model.enums.SuitableForEnum;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HikingTrailMapper {

    @Mapping(target = "trailName", expression = "java(hikingTrailEntity.getStartPoint() + ' ' + '-' + ' ' + hikingTrailEntity.getEndPoint())")
    HikingTrailBasicDto hikingTrailEntityToHikingTrailBasicDto(HikingTrailEntity hikingTrailEntity);

    @Mapping(source = "trailDifficulty.level", target = "trailDifficulty")
    @Mapping(source = "seasonVisited.value", target = "seasonVisited")
    @Mapping(source = "waterAvailable.value", target = "waterAvailable")
    @Mapping(source = "activity", target = "activity")
    HikingTrailDetailsDto hikingTrailEntityToHikingTrailDetailsDto(HikingTrailEntity hikingTrailEntity);

    default List<String> suitableForEnumToStringValue (List<SuitableForEnum> activities) {
        return activities
                .stream()
                .map(SuitableForEnum::getValue)
                .toList();
    }
}
