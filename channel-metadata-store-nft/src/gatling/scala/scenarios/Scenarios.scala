package scenarios

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

object TestScenario {

  val status_scenario = peak_load("private_status", "/private/status")
  val metrics_scenario = peak_load("private_metrics", "/private/metrics")

  def peak_load(requestName: String, requestEndpoint: String) = scenario("peak load test for " + requestName + " endpoint")
    .exec(http(requestName)
      .get(requestEndpoint)
      .header("Authorization", "Basic Y21zOmNtc3Bhc3M=")
      .check(status.is(200)))
    .inject(constantUsersPerSec(10) during (5 minutes))
    .throttle(reachRps(10) in (30 seconds),
      holdFor(5 minutes),
      reachRps(0) in (30 seconds));

  val update_db_scenario = scenario("Update DB Scenario")
    .exec(
      http("post_api_channel_metadata_GB")
        .post("/api/channel-metadata/GB")
        .header("Content-Type", "application/json")
        .header("Authorization", "Basic Y21zOmNtc3Bhc3M=")
        .body(
          StringBody(
            """
              {
                "countryCode": "GB",
                "metadata": [
                  {
                    "name": "AT One",
                    "language": "English",
                    "type": "News"
                  }
                ],
                "product": "exampleProduct"
              }
            """
          )
        ).asJson
        .check(status.is(200))
    )
    .inject(constantUsersPerSec(10) during (5 minutes))
    .throttle(
      reachRps(10) in (30 seconds),
      holdFor(5 minutes),
      reachRps(0) in (30 seconds)
    )

  val get_gb_from_db_scenario = scenario("GET from DB Scenario")
    .exec(
      http("get_api_channel_metadata_GB")
        .get("/api/channel-metadata/GB")
        .header("Content-Type", "application/json")
        .header("Authorization", "Basic Y21zOmNtc3Bhc3M=")
        .check(status.is(200))
        .check(jsonPath("$.countryCode").is("GB"))
        .check(jsonPath("$.product").is("exampleProduct"))
        .check(jsonPath("$.metadata[0].name").is("AT One"))
        .check(jsonPath("$.metadata[0].language").is("English"))
        .check(jsonPath("$.metadata[0].type").is("News"))
    )
    .inject(constantUsersPerSec(10) during (5 minutes))
    .throttle(
      reachRps(10) in (30 seconds),
      holdFor(5 minutes),
      reachRps(0) in (30 seconds)
    )

  val get_gb_whilst_failure_simulation_ongoing = scenario("Peak load under failure getting GB channel metadata")
    .exec(
      http("get_api_channel_metadata_GB")
        .get("/api/channel-metadata/GB")
        .header("Content-Type", "application/json")
        .header("Authorization", "Basic Y21zOmNtc3Bhc3M=")
        .check(status.is(200))
        .check(jsonPath("$.countryCode").is("GB"))
        .check(jsonPath("$.product").is("exampleProduct"))
        .check(jsonPath("$.metadata[0].name").is("AT One"))
        .check(jsonPath("$.metadata[0].language").is("English"))
        .check(jsonPath("$.metadata[0].type").is("News"))
        .check(responseTimeInMillis.lte(2200))
    )
    .inject(
      constantUsersPerSec(20) during (5 minutes)
    ).throttle(
      reachRps(40) in (30 seconds),
      holdFor(5 minutes),
      reachRps(0) in (30 seconds)
    )

  def postPrimingScenario(countryCode: String) = {
    val scenarioName = s"POST $countryCode metadata"
    val endpoint = s"/api/channel-metadata/$countryCode"

    scenario(scenarioName).exec(
      http(s"POST /api/channel-metadata/$countryCode")
        .post(endpoint)
        .header("Content-Type", "application/json")
        .header("Authorization", "Basic Y21zOmNtc3Bhc3M=")
        .body(
          StringBody(
            s"""
          {
            "countryCode": "$countryCode",
            "metadata": [
              {
                "name": "AT One",
                "language": "English",
                "type": "News"
              }
            ],
            "product": "exampleProduct"
          }
          """
          )
        ).asJson
        .check(status.is(200))
    ).inject(atOnceUsers(5))
  }

  def postDuringDowntime(countryCode: String) = {
    val scenarioName = s"POST $countryCode metadata"
    val endpoint = s"/api/channel-metadata/$countryCode"

    scenario(scenarioName).exec(
      http(s"POST /api/channel-metadata/$countryCode")
        .post(endpoint)
        .header("Content-Type", "application/json")
        .header("Authorization", "Basic Y21zOmNtc3Bhc3M=")
        .body(
          StringBody(
            s"""
          {
            "countryCode": "$countryCode",
            "metadata": [
              {
                "name": "AT One",
                "language": "English",
                "type": "News"
              }
            ],
            "product": "exampleProduct"
          }
          """
          )
        ).asJson
    ).inject(constantUsersPerSec(10) during (2 minutes))
  }

  val extended_get_gb_from_db_scenario = scenario("GET from DB Scenario")
    .exec(
      http("get_api_channel_metadata_GB")
        .get("/api/channel-metadata/GB")
        .header("Content-Type", "application/json")
        .header("Authorization", "Basic Y21zOmNtc3Bhc3M=")
        .check(status.is(200))
        .check(jsonPath("$.countryCode").is("GB"))
        .check(jsonPath("$.product").is("exampleProduct"))
        .check(jsonPath("$.metadata[0].name").is("AT One"))
        .check(jsonPath("$.metadata[0].language").is("English"))
        .check(jsonPath("$.metadata[0].type").is("News"))
    )
    .inject(constantUsersPerSec(100) during (30 minutes))
    .throttle(
      reachRps(100) in (30 seconds),
      holdFor(30 minutes),
      reachRps(0) in (30 seconds)
    )

}