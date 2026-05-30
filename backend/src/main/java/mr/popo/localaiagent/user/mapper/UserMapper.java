package mr.popo.localaiagent.user.mapper;

import mr.popo.localaiagent.user.domain.Role;
import mr.popo.localaiagent.user.domain.User;
import mr.popo.localaiagent.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @org.mapstruct.Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    UserDto toDto(User user);

    @Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream().map(Role::getName).collect(Collectors.toSet());
    }
}
