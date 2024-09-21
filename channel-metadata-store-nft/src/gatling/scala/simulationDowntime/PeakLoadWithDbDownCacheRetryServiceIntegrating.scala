package simulationDowntime

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scenarios.TestScenario
import scala.language.postfixOps
import scala.sys.process._

class PeakLoadWithDbDownCacheRetryServiceIntegrating extends Simulation {
  val host = "http://localhost:80"

  val httpProtocol = http.baseUrl(host)

  val scenarios = TestScenario.get_gb_whilst_failure_simulation_ongoing :: List("GB", "US", "DE", "FR").map { countryCode =>
    TestScenario.postDuringDowntime(countryCode)
  }
  Process("./src/gatling/scala/scripts/failure_simulation_db_down.sh").run()

  setUp(scenarios)
    .protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile(99).lt(3500),
    )
}