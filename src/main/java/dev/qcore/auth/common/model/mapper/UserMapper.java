package dev.qcore.auth.common.model.mapper;

import dev.qcore.auth.common.constants.JwtConstants;
import dev.qcore.auth.common.model.dto.request.RegisterRequest;
import dev.qcore.auth.common.model.dto.response.TokenResponse;
import dev.qcore.auth.common.model.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "role", expression = "java(dev.qcore.auth.common.enums.UserRole.USER)")
    @Mapping(target = "password", ignore = true)
    UserEntity toUserEntity(RegisterRequest request);

    @Mapping(target = "token", source = "token")
    @Mapping(target = "tokenType", constant = JwtConstants.TOKEN_TYPE_BEARER)
    @Mapping(target = "expiresIn", constant = JwtConstants.EXPIRES_IN_SECONDS_DEFAULT)
    @Mapping(target = "userId", ignore = true)
    TokenResponse toTokenResponse(String token);
}