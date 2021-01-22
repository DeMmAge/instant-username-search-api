package com.umutcanbolat.instantusernamesearchapi.helper;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.GetRequest;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CheckServiceHelper {
  public GetRequest getBaseRequest(String url) {
    return Unirest.get(url)
        .header("Connection", "keep-alive")
        .header("Upgrade-Insecure-Requests", "1")
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/83.0.4103.116 Safari/537.36")
        .header(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        .header("Accept-Encoding", "gzip, deflate")
        .header("Accept-Language", "en-US;q=1");
  }
}
