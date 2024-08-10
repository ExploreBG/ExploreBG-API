package bg.exploreBG.model.user;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class ExploreBgUserDetails implements UserDetails {

    private final Collection<GrantedAuthority> authorities;
    private final String email;
    private final String password;
    private final String profileName;

    public ExploreBgUserDetails(String email, String password, String username, Collection<GrantedAuthority> authorities) {
        this.email = email;
        this.password = password;
        this.profileName = username;
        this.authorities = authorities;
    }

    public String getProfileName() {
        return this.profileName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.email;
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
        return true;
    }
}
