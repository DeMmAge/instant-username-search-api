package com.umutcanbolat.instantusernamesearchapi.service.impl;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.umutcanbolat.instantusernamesearchapi.controller.CheckController;
import com.umutcanbolat.instantusernamesearchapi.model.ServiceModel;
import com.umutcanbolat.instantusernamesearchapi.model.ServiceResponseModel;
import com.umutcanbolat.instantusernamesearchapi.model.SiteModel;
import com.umutcanbolat.instantusernamesearchapi.service.CheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

@Service
@Slf4j
public class CheckServiceImpl implements CheckService {
  private static LinkedHashMap<String, SiteModel> sitesMap;
  private static List<ServiceModel> serviceList;

  public CheckServiceImpl() {
    // read sites.json file
    InputStream in = CheckController.class.getResourceAsStream("/static/sites.json");
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

    // parse json to model
    Gson gson = new Gson();
    Type mapType = new TypeToken<LinkedHashMap<String, SiteModel>>() {}.getType();
    sitesMap = gson.fromJson(reader, mapType);

    // prepare serviceList
    serviceList = new ArrayList<>();
    for (Entry<String, SiteModel> site : sitesMap.entrySet()) {
      try {
        String serviceName = site.getValue().getService();
        URI serviceEndpoint = new URI("/" + site.getKey().toLowerCase());

        serviceList.add(
            ServiceModel.builder()
                .service(serviceName)
                .endpoint(serviceEndpoint + "/{username}")
                .build());
      } catch (URISyntaxException e) {
        log.error("Cannot build service check URL", e);
      }
    }
  }

  @Override
  public ServiceResponseModel check(String service, String username) {
    try {
      SiteModel site = sitesMap.get(service.toLowerCase());
      if (site != null) {
        String url = site.getUrl().replace("{}", username);
        HttpResponse<String> response =
            Unirest.get(url)
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header(
                    "User-Agent",
                    site.getUserAgent() != null
                        ? site.getUserAgent()
                        : "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36")
                .header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate")
                .header("Accept-Language", "en-US;q=1")
                .asString();
        log.info("checking " + service + " for " + username);
        log.info(url);
        log.info("status: " + response.getStatus());

        boolean available = false;

        /*
         * Error Types
         * 0: returns HTTP 4xx response when the username is available.
         * 1: still returns HTTP 2xx when the username is not taken, so we check the response body for errorMsg.
         * 2: returns HTTP 4xx if the username is not taken or disabled.
         * 	  Since disabled usernames cannot be taken again, we check the response body.
         */
        if (site.getErrorType() == 0) {
          if (response.getStatus() != 200) {
            available = true;
          }
        } else if (site.getErrorType() == 1) {
          if (response.getBody().contains(site.getErrorMsg())) {
            available = true;
          }
        } else if (site.getErrorType() == 2) {
          if (response.getStatus() != 200 && response.getBody().contains(site.getErrorMsg())) {
            available = true;
          }
        }
        return new ServiceResponseModel(site.getService(), url, available);
      }
      // service not found
      return new ServiceResponseModel("Service " + service + " is not supported yet :/");
    } catch (Exception ex) {
      return new ServiceResponseModel(ex.getMessage());
    }
  }

  @Override
  public List<ServiceModel> getServices() {
    return serviceList;
  }
}
