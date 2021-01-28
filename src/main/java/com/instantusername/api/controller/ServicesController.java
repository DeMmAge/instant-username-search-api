package com.instantusername.api.controller;

import com.instantusername.api.model.ServiceModel;
import com.instantusername.api.service.CheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/services")
public class ServicesController {
  @Autowired private CheckService checkService;

  @RequestMapping
  @Cacheable("services")
  public List<ServiceModel> getServicesList() {
    return checkService.getServices();
  }
}
