Feature: Channel Metadata API - returning and accepting metadata.

  Scenario: Save GB channel metadata
    Given the database is up and running
    When I POST data to "/api/channel-metadata/GB" with body and auth header "Basic Y21zOmNtc3Bhc3M="
    And the status code should be 200
    And the response should contain "Data successfully posted"

  Scenario: Save GB channel metadata and return the same metadata
    Given the database is up and running
    When I POST data to "/api/channel-metadata/GB" with body and auth header "Basic Y21zOmNtc3Bhc3M="
    And the status code should be 200
    And the response should contain "Data successfully posted"
    And I GET data from "/api/channel-metadata/GB" with auth header "Basic Y21zOmNtc3Bhc3M="
    Then the status code should be 200
    And the response should contain channel metadata

  Scenario: Force update all from cache with GB channel metadata
    Given the database is up and running
    And I POST data to "/api/channel-metadata/GB" with body and auth header "Basic Y21zOmNtc3Bhc3M="
    And the status code should be 200
    And the response should contain "Data successfully posted"
    When I POST data to "/api/channel-metadata/force-update-all" without body
    And the status code should be 200
    And the response should contain "Successfully updated the database with all cached metadata."

  Scenario: Receive security failure when saving GB channel metadata
    Given the database is up and running
    When I POST data to "/api/channel-metadata/GB" with invalid credentials "Basic sfsfs="
    Then the status code should be 401
    And the response should contain "Invalid or missing credentials"