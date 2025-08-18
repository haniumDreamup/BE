package com.bifai.reminder.bifai_backend.security.auth;

import com.bifai.reminder.bifai_backend.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class CustomUserDetails implements UserDetails, OAuth2User {
  
  private final Long id;
  private final String email;
  private final String password;
  private final String name;
  private final String role;
  private final boolean enabled;
  private Map<String, Object> attributes;
  
  public CustomUserDetails(User user) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.password = user.getPasswordHash() != null ? user.getPasswordHash() : "";
    this.name = user.getName();
    this.role = "USER"; // Default role for all users
    this.enabled = user.isActive();
  }
  
  public CustomUserDetails(User user, Map<String, Object> attributes) {
    this(user);
    this.attributes = attributes;
  }
  
  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
  }
  
  @Override
  public String getPassword() {
    return password;
  }
  
  @Override
  public String getUsername() {
    return email;
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
  
  // OAuth2User methods
  @Override
  public Map<String, Object> getAttributes() {
    return attributes;
  }
  
  @Override
  public String getName() {
    return name;
  }
  
  public static CustomUserDetails create(User user) {
    return new CustomUserDetails(user);
  }
  
  public static CustomUserDetails create(User user, Map<String, Object> attributes) {
    return new CustomUserDetails(user, attributes);
  }
}