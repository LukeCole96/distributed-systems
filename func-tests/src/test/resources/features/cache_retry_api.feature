Feature: Cache-Retry API - consumes from kafka and triggers retries on cms cache when there's been DB failure, and get downtime logs

  @cacheRetry
  Scenario: Trigger a cache retry
    Given the database is up and running
    When I GET data from "/trigger-cache-retry" with auth header "Basic Y3JzOmNyc3Bhc3M="
    Then the status code should be 200
    And the response should contain "Triggering a DB-write retry for channel-metadata-store"

  @cacheRetry
  Scenario: Get downtime logs from cache-retry
    Given the database is up and running
    When I GET data from "/get-downtime-logs" with auth header "Basic Y3JzOmNyc3Bhc3M="
    Then the status code should be 200
    And the response should contain "downtimeTimestamp"

  @cacheRetry
  Scenario: Receive security failure when trying to retry cache
    Given I GET data from "/trigger-cache-retry" with invalid credentials "Basic banana="
    When the status code should be 500
