package com.instantusername.api.service;

import com.instantusername.api.model.ServiceModel;
import com.instantusername.api.model.ServiceResponseModel;

import java.util.List;

public interface CheckService {
  ServiceResponseModel check(String service, String username);

  List<ServiceModel> getServices();
}
