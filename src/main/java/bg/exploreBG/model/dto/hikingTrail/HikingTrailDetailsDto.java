package bg.exploreBG.model.dto.hikingTrail;

import bg.exploreBG.model.dto.CommentsDto;
import bg.exploreBG.model.dto.accommodation.AccommodationBasicDto;
import bg.exploreBG.model.dto.destination.DestinationBasicDto;

import java.util.List;

public record HikingTrailDetailsDto(
        Long id,
        String startPoint,
        String endPoint,
        double totalDistance,
        String trailInfo,
        String imageUrl,
        String seasonVisited,
        String waterAvailable,
        List<AccommodationBasicDto> availableHuts,
        int trailDifficulty,
        List<String> activity,
        List<CommentsDto> comments,
        double elevationGained,
        String nextTo,
        List<DestinationBasicDto> destinations) {
}
