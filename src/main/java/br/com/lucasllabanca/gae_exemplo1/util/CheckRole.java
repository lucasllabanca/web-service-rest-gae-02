package br.com.lucasllabanca.gae_exemplo1.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class CheckRole {

    public boolean hasRoleAdmin(Authentication authentication) {
        return hasRole(authentication, "ROLE_ADMIN");
    }

    public boolean hasRoleUser(Authentication authentication) {
        return hasRole(authentication, "ROLE_USER");
    }

    private boolean hasRole(Authentication authentication, String role) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        for (GrantedAuthority grantedAuthority : userDetails.getAuthorities()) {
            if (grantedAuthority.getAuthority().equals(role))
                return true;
        }
        return false;
    }
}
