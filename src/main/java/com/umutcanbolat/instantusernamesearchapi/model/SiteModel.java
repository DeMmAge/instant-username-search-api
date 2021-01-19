package com.umutcanbolat.instantusernamesearchapi.model;

import lombok.AllArgsConstructor;
import lombok.Value;

/** This model is used for parsing sites.json file */
@Value
@AllArgsConstructor
public class SiteModel {
  private String service;
  private String url;
  private String urlRegister;
  private int errorType;
  private String errorMsg;
  private String userAgent;
}
