package bg.exploreBG.model.dto.image;

import bg.exploreBG.model.dto.user.UserBasicInfo;
import bg.exploreBG.model.enums.StatusEnum;
import bg.exploreBG.serializer.StatusEnumSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public record ImageBasicDto(
        Long id,
        String imageUrl,
        @JsonSerialize(using = StatusEnumSerializer.class)
        @JsonProperty(value = "imageStatus")
        StatusEnum status,
        UserBasicInfo reviewedBy
) {
}
