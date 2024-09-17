package simulationDowntime

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scenarios.TestScenario

import scala.sys.process._
import scala.language.postfixOps

class PeakLoadWithSingleHazelcastDown extends Simulation {
  val host = "http://localhost:80"
  val httpProtocol = http.baseUrl(host)

  val scenarios = TestScenario.get_gb_whilst_failure_simulation_ongoing :: List("GB", "US", "DE", "FR").map { countryCode =>
    TestScenario.postPrimingScenario(countryCode)
  }

  Process("./src/gatling/scala/scripts/failure_simulation_hazelcast.sh").run()

  setUp(scenarios)
    .protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(99).lt(600),
      forAll.successfulRequests.percent.gt(90)
    )
}