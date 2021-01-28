package com.instantusername.api.model;

import lombok.Builder;
import lombok.Value;

/** This model is used for exposing the available (checkable) services list to the client */
@Value
@Builder
public class ServiceModel {
  String service;
  String endpoint;
}
