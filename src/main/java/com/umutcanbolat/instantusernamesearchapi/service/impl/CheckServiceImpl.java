package com.umutcanbolat.instantusernamesearchapi.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;
import com.umutcanbolat.instantusernamesearchapi.helper.CheckServiceHelper;
import com.umutcanbolat.instantusernamesearchapi.model.ErrorType;
import com.umutcanbolat.instantusernamesearchapi.model.ServiceModel;
import com.umutcanbolat.instantusernamesearchapi.model.ServiceResponseModel;
import com.umutcanbolat.instantusernamesearchapi.model.ServiceResponseModel.ServiceResponseModelBuilder;
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
  private static final String SITES_PATH = "/static/sites.json";
  private static final ObjectMapper objectMapper = new ObjectMapper();

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
    log.debug("Checking `{}` for the availability of username `{}`", service, username);
    SiteModel site = sitesMap.get(service.toLowerCase());

    ServiceResponseModelBuilder responseBuilder = ServiceResponseModel.builder();
    if (site == null) {
      log.warn("Service `{}` is not configured. Aborting the request..", service);
      return responseBuilder.message("Service " + service + " is not supported yet :/").build();
    }

    final String serviceName = site.getService();
    final String requestUrl = site.getUrl().replace("{}", username);
    final ErrorType errorType = site.getErrorType();
    responseBuilder.service(serviceName);
    responseBuilder.url(requestUrl);

    try {
      HttpResponse<String> remoteResponse = sendRemoteRequest(site, requestUrl);
      boolean available = false;

      if (ErrorType.HTTP.equals(errorType)) {
        log.debug(
            "For service `{}`, errorType is {} and response status is {}",
            serviceName,
            errorType,
            remoteResponse.getStatus());
        if (remoteResponse.getStatus() != 200) {
          available = true;
        }
      } else if (ErrorType.TEXT.equals(errorType)) {
        log.debug("For service `{}`, errorType is {}.", serviceName, errorType);
        available =
            site.getErrorMsg()
                .map(
                    msg -> {
                      boolean check = remoteResponse.getBody().contains(msg);
                      log.debug(
                          "Seems like the response body contains error message `{}` is {}",
                          msg,
                          check);
                      return check;
                    })
                .orElseGet(
                    () -> {
                      log.error(
                          "Configuration error. `errorMsg` field should not be empty when the `errorType` is set to TEXT. Misconfiguration in site data of `{}`.",
                          serviceName);
                      return false;
                    });
      }

      log.debug(
          "Availability of username `{}` at `{}` seems to be {}", username, serviceName, available);
      return responseBuilder.available(available).build();
    } catch (UnirestException ex) {
      log.warn("Remote request was unsuccessful  for service: " + serviceName, ex);
      return responseBuilder.message("An error occured while checking the service.").build();
    }
  }

  @Override
  public List<ServiceModel> getServices() {
    log.debug("Returning the list of the services.");
    return serviceList;
  }

  private LinkedHashMap<String, SiteModel> readSitesData() throws IOException {
    InputStream in = getClass().getResourceAsStream(SITES_PATH);
    BufferedReader reader = new BufferedReader(new InputStreamReader(in));

    LinkedHashMap<String, SiteModel> map =
        objectMapper.readValue(reader, new TypeReference<LinkedHashMap<String, SiteModel>>() {});

    log.info("Sites data read. Number of services read from configuration is: {}", map.size());
    return map;
  }

  private List<ServiceModel> prepareServices() {
    List<ServiceModel> services = new ArrayList<>();

    for (Entry<String, SiteModel> site : this.sitesMap.entrySet()) {
      String serviceName = site.getValue().getService();

      try {
        URI serviceEndpoint = new URI("/check/" + site.getKey().toLowerCase());

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

    log.info("Prepared the list of services. Total number of services is: {}", services.size());
    return services;
  }

  private HttpResponse<String> sendRemoteRequest(SiteModel site, String url)
      throws UnirestException {
    GetRequest request = CheckServiceHelper.getBaseRequest(url);

    if (site.getUserAgent().isPresent()) {
      request.header("User-Agent", site.getUserAgent().get());
    }

    log.debug("Sending GET request to: {}", url);
    try {
      log.debug("with headers: {}", objectMapper.writeValueAsString(request.getHeaders()));
    } catch (JsonProcessingException e) {
      log.warn("Could not serialize request headers.", e);
    }
    return request.asString();
  }
}
