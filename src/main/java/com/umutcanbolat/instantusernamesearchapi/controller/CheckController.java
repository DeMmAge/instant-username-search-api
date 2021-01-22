package com.umutcanbolat.instantusernamesearchapi.controller;

import com.umutcanbolat.instantusernamesearchapi.model.ServiceResponseModel;
import com.umutcanbolat.instantusernamesearchapi.service.CheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/check")
public class CheckController {
  @Autowired private CheckService checkService;

  @RequestMapping("/{service}/{username}")
  @Cacheable("availabilities")
  public ServiceResponseModel searchUsername(
      @PathVariable String service, @PathVariable String username) {
    return checkService.check(service, username);
  }
}
