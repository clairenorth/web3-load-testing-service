import ch.qos.logback.classic.{Level, LoggerContext}
import io.gatling.core.Predef._
import io.gatling.http.Predef._
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.Random

// Note: No response caching will hurt performance (future improvement)

class ThroughputBaselineSimulation extends Simulation {

  val context: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
  context.getLogger("io.gatling.http").setLevel(Level.valueOf("INFO"))

  val feeder =  Iterator.continually(Map("blockParameter" -> (Random.between(9228709,9228783).toHexString),
  "transactionIndexPosition" -> ("0x" + (Random.between(0,5)).toString)))

  // executes before simulation starts
  before {
      println(s"${getClass.getName} starting...")
    }

    after {
      println("${getClass.getName} completed.")
    }

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .acceptEncodingHeader("Content-Type: application/json")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
    .shareConnections

  val scn1 =
    scenario("blockByNumber")
      .repeat(2) {
        feed(feeder) // A scenario is a chain of requests and pauses
          .exec(http("blockByNumber")
            .get("/blockByNumber/${blockParameter}/false"))
          .pause(2000.milliseconds)
      }



  val scn2 = scenario("transactionByBlockNumberAndIndex") // A scenario is a chain of requests and pauses
    .repeat(2) {
      feed(feeder)
        .exec(http("transactionByBlockNumberAndIndex")
          .get("/transactionByBlockNumberAndIndex/${blockParameter}/${transactionIndexPosition}"))
        .pause(2000.milliseconds)
    }


  // 1 user - 2 requests. 250 * 2 = 500 RPS
  setUp(scn1.inject(rampUsers(250) during(30 seconds))
    .throttle(reachRps(500) in (1 minutes))
    .protocols(httpProtocol).andThen(scn2.inject(constantUsersPerSec(250) during(30 seconds))
    .throttle(reachRps(500) in (1 minutes)).protocols(httpProtocol)))
}

