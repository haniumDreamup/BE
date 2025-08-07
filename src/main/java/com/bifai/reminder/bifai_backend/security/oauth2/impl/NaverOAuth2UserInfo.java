package com.bifai.reminder.bifai_backend.security.oauth2.impl;

import com.bifai.reminder.bifai_backend.security.oauth2.OAuth2UserInfo;

import java.util.Map;

public class NaverOAuth2UserInfo implements OAuth2UserInfo {

  private Map<String, Object> attributes;

  public NaverOAuth2UserInfo(Map<String, Object> attributes) {
    this.attributes = (Map<String, Object>) attributes.get("response");
  }

  @Override
  public String getId() {
    return (String) attributes.get("id");
  }

  @Override
  public String getEmail() {
    return (String) attributes.get("email");
  }

  @Override
  public String getName() {
    return (String) attributes.get("name");
  }

  @Override
  public String getImageUrl() {
    return (String) attributes.get("profile_image");
  }
}