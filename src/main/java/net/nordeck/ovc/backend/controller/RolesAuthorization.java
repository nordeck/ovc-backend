package net.nordeck.ovc.backend.controller;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;


@Component("rolesAuthorization")
public class RolesAuthorization
{

    @Value("${api.basic-access-role}")
    @Getter
    protected String basicAccessRole;

    public boolean hasBasicAccessRole()
    {
        if (!SecurityContextHolder.getContext().getAuthentication().isAuthenticated())
        {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        if (StringUtils.isEmpty(basicAccessRole))
        {
            return true;
        }
        return authorities.stream().anyMatch(a -> a.getAuthority().equalsIgnoreCase(basicAccessRole));
    }

}

