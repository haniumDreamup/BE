package com.bifai.reminder.bifai_backend.security.oauth2.impl;

import com.bifai.reminder.bifai_backend.security.oauth2.OAuth2UserInfo;

import java.util.Map;

public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

  private Map<String, Object> attributes;

  public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
    this.attributes = attributes;
  }

  @Override
  public String getId() {
    return (String) attributes.get("sub");
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
    return (String) attributes.get("picture");
  }
}