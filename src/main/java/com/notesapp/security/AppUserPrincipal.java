package com.notesapp.security;

import java.util.Collection;
import java.util.List;

import com.notesapp.entity.Role;
import com.notesapp.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * The authenticated user as seen by Spring Security. Controllers receive it via
 * {@code @AuthenticationPrincipal} and pass {@link #getId()} to the services.
 */
public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Role role;
    private final boolean enabled;

    public AppUserPrincipal(Long id, String username, String password, Role role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.enabled = enabled;
    }

    public static AppUserPrincipal from(User user) {
        return new AppUserPrincipal(user.getId(), user.getUsername(), user.getPassword(),
                user.getRole(), user.isEnabled());
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
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

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
