package bg.exploreBG.model.dto.accommodation;

public record AccommodationBasicDto(
        Long id,
        String accommodationName,
        String imageUrl,
        String nextTo
) {
}
