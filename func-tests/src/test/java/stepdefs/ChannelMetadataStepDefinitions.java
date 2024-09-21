package stepdefs;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class ChannelMetadataStepDefinitions {

    private Response response;
    private String host;

    private void setHost(String endpoint) {
        host = endpoint.startsWith("/api/channel-metadata") ? "http://localhost:80" : "http://localhost:90";
    }

    @Given("the database is up and running")
    public void the_database_is_up_and_running() {
    }

    @When("I POST data to {string} with body and auth header {string}")
    public void post_data_to_with_body(String endpoint, String authHeader) {
        setHost(endpoint);

        String requestBody = """
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
        """;

        response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .body(requestBody)
                .post(host + endpoint);

        response.prettyPrint();
    }

    @When("I POST malformed JSON to {string} with auth header {string}")
    public void post_malformed_json_to(String endpoint, String authHeader) {
        setHost(endpoint);

        String malformedJson = """
                {
                        {
                            "name": "AT One",
                            "language": "English",
                            "type": "News"
                        }
                   oduct": "exampleProduct"
                }
        """;

        response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .body(malformedJson)
                .post(host + endpoint);

        response.prettyPrint();
    }

    @When("I POST data with invalid attribute to {string} with auth header {string}")
    public void post_data_with_invalid_attribute_to(String endpoint, String authHeader) {
        setHost(endpoint);

        String invalidRequest = """
            {
                "invalidCountryCode": "GB",
                "metadata": [
                    {
                        "name": "AT One",
                        "language": "English",
                        "type": "News"
                    }
                ],
                "product": "exampleProduct"
            }
        """;

        response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .body(invalidRequest)
                .post(host + endpoint);

        response.prettyPrint();
    }

    @When("I POST data with malformed field to {string} with auth header {string}")
    public void post_data_with_malformed_field_to(String endpoint, String authHeader) {
        setHost(endpoint);

        String malformedFieldRequest = """
            {
                "countryCode": "GB",
                "metadata": [
                    {
                        "name": "AT One",
                        "language": "123,
                        "type": "News"
                    }
                ],
                "product": "exampleProduct"
            }
        """;

        response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .body(malformedFieldRequest)
                .post(host + endpoint);

        response.prettyPrint();
    }

    @When("I POST data to {string} with invalid credentials {string}")
    public void post_data_to_with_invalid_credentials(String endpoint, String auth) {
        setHost(endpoint);

        String requestBody = """
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
        """;

        response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", auth)
                .body(requestBody)
                .post(host + endpoint);

        response.prettyPrint();
    }

    @When("I GET data from {string} with invalid credentials {string}")
    public void get_data_to_with_invalid_credentials(String endpoint, String auth) {
        setHost(endpoint);

        response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", auth)
                .get(host + endpoint);

        response.prettyPrint();
    }

    @When("I POST data to {string} without body")
    public void post_data_to_without_body(String endpoint) {
        setHost(endpoint);

        response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic Y21zOmNtc3Bhc3M=")
                .post(host + endpoint);

        response.prettyPrint();
    }

    @And("I GET data from {string} with auth header {string}")
    public void get_data_from(String endpoint, String authHeader) {
        setHost(endpoint);

        response = given()
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .get(host + endpoint);

        response.prettyPrint();
    }

    @Then("the response should contain {string}")
    public void the_response_should_contain(String expectedMessage) {
        assertThat(response.getBody().asString(), containsString(expectedMessage));
    }

    @Then("the status code should be {int}")
    public void the_status_code_should_be(int statusCode) {
        await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    response.then().statusCode(statusCode);
                });
    }

    @Then("the response should contain channel metadata")
    public void the_response_should_contain_channel_metadata() {
        await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String responseBody = response.getBody().asString();
                    assertThat(responseBody, containsString("countryCode"));
                    assertThat(responseBody, containsString("metadata"));
                    assertThat(responseBody, containsString("product"));
                });
    }
}
