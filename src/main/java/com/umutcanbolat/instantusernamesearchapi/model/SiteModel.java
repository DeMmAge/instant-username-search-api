package com.umutcanbolat.instantusernamesearchapi.model;

import lombok.AllArgsConstructor;
import lombok.Value;

/** This model is used for parsing sites.json file */
@Value
@AllArgsConstructor
public class SiteModel {
  String service;
  String url;
  String urlRegister;
  int errorType;
  String errorMsg;
  String userAgent;
}
