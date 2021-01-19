package com.umutcanbolat.instantusernamesearchapi.controller;

import com.umutcanbolat.instantusernamesearchapi.model.ServiceModel;
import com.umutcanbolat.instantusernamesearchapi.service.CheckService;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Value
@NonFinal
@RestController
@RequestMapping("/services")
public class ServicesController {
  @Autowired CheckService checkService;

  @RequestMapping
  @Cacheable("services")
  public List<ServiceModel> getServicesList() {
    return checkService.getServices();
  }
}
