package se325.assignment01.concert.service.mapper;

import se325.assignment01.concert.common.dto.UserDTO;
import se325.assignment01.concert.service.domain.User;

public class UserMapper {
    static User toDomainModel(UserDTO dtoConcert) {
        User fullUser = new User(
                dtoConcert.getUsername(),
                dtoConcert.getPassword()
        );

        return fullUser;
    }

    static UserDTO toDto(User user) {
        UserDTO dtoUser = new UserDTO(
                user.getUsername(),
                user.getPassword()
        );

        return dtoUser;
    }
}
