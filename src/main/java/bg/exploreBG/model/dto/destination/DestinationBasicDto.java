package bg.exploreBG.model.dto.destination;

public record DestinationBasicDto(
        Long id,
        String destinationName,
        String imageUrl,
        String nextTo
) {
}
