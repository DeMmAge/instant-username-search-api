package com.instantusername.api.model;

public enum ErrorType {
  /**
   * Used for the sites that returns HTTP 2XX when a username is taken and HTTP 4XX when it is
   * available.
   */
  HTTP,
  /**
   * For the sites that always returns HTTP 2XX. So we need to search for a specific error text in
   * the response body.
   */
  TEXT
}
