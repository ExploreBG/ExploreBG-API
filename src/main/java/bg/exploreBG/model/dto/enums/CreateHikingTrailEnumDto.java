package bg.exploreBG.model.dto.enums;

import bg.exploreBG.model.enums.DifficultyLevelEnum;
import bg.exploreBG.model.enums.SeasonEnum;
import bg.exploreBG.model.enums.SuitableForEnum;
import bg.exploreBG.model.enums.WaterAvailabilityEnum;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class CreateHikingTrailEnumDto {
    private final Set<String> seasonVisited;
    private final Set<String> waterAvailable;
    private final int[] trailDifficulty;
    private final Set<String> activity;

    public CreateHikingTrailEnumDto() {
        this.seasonVisited = setSeason();
        this.waterAvailable = setWaterAvailable();
        this.trailDifficulty = setTrailDifficulty();
        this.activity = setSuitableFor();
    }

    private Set<String> setSeason() {
        return Arrays.stream(SeasonEnum.values())
                .map(SeasonEnum::getValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> setWaterAvailable() {
        return Arrays.stream(WaterAvailabilityEnum.values())
                .map(WaterAvailabilityEnum::getValue)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private int[] setTrailDifficulty() {
        return Arrays.stream(DifficultyLevelEnum.values())
                .mapToInt(DifficultyLevelEnum::getLevel)
                .toArray();

    }

    private Set<String> setSuitableFor() {
        return Arrays.stream(SuitableForEnum.values())
                .map(SuitableForEnum::getValue)
                .collect(Collectors.toSet());
    }

    public Set<String> getSeasonVisited() {
        return seasonVisited;
    }

    public Set<String> getWaterAvailable() {
        return waterAvailable;
    }

    public int[] getTrailDifficulty() {
        return trailDifficulty;
    }

    public Set<String> getActivity() {
        return activity;
    }
}
