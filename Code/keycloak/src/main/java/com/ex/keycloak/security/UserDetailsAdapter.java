package com.ex.keycloak.security;

import com.ex.keycloak.constants.UserStatus;
import com.ex.keycloak.domain.Role;
import com.ex.keycloak.domain.User;
import com.ex.keycloak.dto.UserInfo;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserDetailsAdapter implements UserDetails {

    private final transient UserInfo userInfo;
    private final Collection<GrantedAuthority> authorities;



    public UserDetailsAdapter(UserInfo userInfo){

        this.userInfo = userInfo;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + userInfo.getRoles()));
    }

    public UserInfo getUser() {
        return userInfo;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return userInfo.getUsername();
    }


    @Override
    public boolean isEnabled() {
        return userInfo.getStatus() == UserStatus.ACTIVE;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}
