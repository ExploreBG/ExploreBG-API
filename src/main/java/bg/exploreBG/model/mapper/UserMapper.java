package bg.exploreBG.model.mapper;

import bg.exploreBG.model.dto.user.UserDataDto;
import bg.exploreBG.model.dto.user.UserDetailsDto;
import bg.exploreBG.model.dto.user.UserDetailsOwnerDto;
import bg.exploreBG.model.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(source = "userImage.imageUrl", target = "imageUrl")
    @Mapping(source = "gender.value", target = "gender")
    UserDetailsDto userEntityToUserDetailsDto(UserEntity user);

    @Mapping(source = "userImage.imageUrl", target = "imageUrl")
    @Mapping(source = "gender.value", target = "gender")
    UserDetailsOwnerDto userEntityToUserDetailsOwnerDto(UserEntity user);

    UserDataDto userEntityToUserDataDto(UserEntity userEntity);
}
