package com.nithingodugu.ecommerce.authservice.Util;

import com.nithingodugu.ecommerce.authservice.domain.entity.User;
import com.nithingodugu.ecommerce.authservice.dto.UserProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class Mappers {

    public UserProfileResponse mapUserToUserProfileResponse(User user){
        return new UserProfileResponse(
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
