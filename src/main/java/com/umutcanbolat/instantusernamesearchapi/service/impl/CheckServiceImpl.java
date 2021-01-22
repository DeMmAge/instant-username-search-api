package com.umutcanbolat.instantusernamesearchapi.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.umutcanbolat.instantusernamesearchapi.model.ErrorType;
import com.umutcanbolat.instantusernamesearchapi.model.ServiceModel;
import com.umutcanbolat.instantusernamesearchapi.model.ServiceResponseModel;
import com.umutcanbolat.instantusernamesearchapi.model.SiteModel;
import com.umutcanbolat.instantusernamesearchapi.service.CheckService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

@Service
@Slf4j
public class CheckServiceImpl implements CheckService {
  private final String SITES_PATH = "/static/sites.json";
  private final ObjectMapper objectMapper = new ObjectMapper();

  // LinkedHashMap to keep the order.
  private LinkedHashMap<String, SiteModel> sitesMap;
  private List<ServiceModel> serviceList;

  public CheckServiceImpl() throws IOException {
    // read and parse sites.json file
    sitesMap = readSitesData();

    // prepare serviceList
    serviceList = prepareServices();
  }

  @Override
  public ServiceResponseModel check(String service, String username) {
    log.trace("Checking `{}` for the availability of username `{}`", service, username);
    try {
      SiteModel site = sitesMap.get(service.toLowerCase());

      if (site == null) {
        log.warn("Service `{}` is not configured. Aborting the request..", service);
        return ServiceResponseModel.builder()
            .message("Service " + service + " is not supported yet :/")
            .build();
      }

      String requestUrl = site.getUrl().replace("{}", username);
      HttpResponse<String> response = sendRemoteRequest(site, requestUrl);

      boolean available = false;

      if (ErrorType.HTTP.equals(site.getErrorType())) {
        log.trace(
            "For service `{}`, errorType is {} and response status is {}",
            service,
            site.getErrorType(),
            response.getStatus());
        if (response.getStatus() != 200) {
          available = true;
        }
      } else if (ErrorType.TEXT.equals(site.getErrorType())) {
        log.trace("For service `{}`, errorType is {}.", service, site.getErrorType());
        available =
            site.getErrorMsg()
                .map(
                    msg -> {
                      boolean check = response.getBody().contains(msg);
                      log.trace(
                          "Seems like the response body contains error message `{}` is {}",
                          msg,
                          check);
                      return check;
                    })
                .orElseGet(
                    () -> {
                      log.error(
                          "Configuration error. `errorMsg` field should not be empty when the `errorType` is set to TEXT. Misconfiguration in site data of `{}`.",
                          site.getService());
                      return false;
                    });
      }

      log.trace(
          "Availability of username `{}` at `{}` seems to be {}", username, service, available);
      return ServiceResponseModel.builder()
          .service(service)
          .url(requestUrl)
          .available(available)
          .build();

    } catch (UnirestException ex) {
      log.warn("Remote request for service `{}` was unsuccessful.", service);
      return ServiceResponseModel.builder()
          .message("An error happened while checking the service.")
          .build();
    }
  }

  @Override
  public List<ServiceModel> getServices() {
    return serviceList;
  }

  private LinkedHashMap<String, SiteModel> readSitesData() throws IOException {
    InputStream in = getClass().getResourceAsStream(SITES_PATH);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

    return objectMapper.readValue(reader, new TypeReference<LinkedHashMap<String, SiteModel>>() {});
  }

  private List<ServiceModel> prepareServices() {
    List<ServiceModel> services = new ArrayList<>();

    for (Entry<String, SiteModel> site : this.sitesMap.entrySet()) {
      String serviceName = site.getValue().getService();

      try {
        URI serviceEndpoint = new URI("/" + site.getKey().toLowerCase());

        services.add(
            ServiceModel.builder()
                .service(serviceName)
                .endpoint(serviceEndpoint + "/{username}")
                .build());
      } catch (URISyntaxException e) {
        log.error("Cannot build a check url for service: " + serviceName, e);
        log.warn("Skipping {}", serviceName);
      }
    }

    return services;
  }

  private HttpResponse<String> sendRemoteRequest(SiteModel site, String url)
      throws UnirestException {
    log.trace("Sending remote request to: {}", url);
    return Unirest.get(url)
        .header("Connection", "keep-alive")
        .header("Upgrade-Insecure-Requests", "1")
        .header(
            "User-Agent",
            site.getUserAgent()
                .orElse(
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36"))
        .header(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        .header("Accept-Encoding", "gzip, deflate")
        .header("Accept-Language", "en-US;q=1")
        .asString();
  }
}
