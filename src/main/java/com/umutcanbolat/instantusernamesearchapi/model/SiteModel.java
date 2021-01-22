package com.umutcanbolat.instantusernamesearchapi.model;

import lombok.Data;

import java.util.Optional;

/** This model is used for parsing sites.json file */
@Data
public class SiteModel {
  String service;
  String url;
  String urlRegister;
  ErrorType errorType;
  Optional<String> errorMsg = Optional.empty();
  Optional<String> userAgent = Optional.empty();

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = Optional.ofNullable(errorMsg);
  }

  public void setUserAgent(String userAgent) {
    this.userAgent = Optional.ofNullable(userAgent);
  }
}
