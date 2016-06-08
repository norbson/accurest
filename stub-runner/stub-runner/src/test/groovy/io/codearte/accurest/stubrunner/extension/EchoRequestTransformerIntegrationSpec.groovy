package io.codearte.accurest.stubrunner.extension

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.jayway.restassured.RestAssured
import org.junit.Rule
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson
import static com.github.tomakehurst.wiremock.client.WireMock.matching
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import static com.jayway.restassured.RestAssured.given
import static org.hamcrest.core.IsEqual.equalTo

/**
 * @author Norbert Krzysztofik
 */

class EchoRequestTransformerIntegrationSpec extends Specification {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort()
                                                                        .extensions(EchoRequestTransformer))

    def 'should return response with data from query params and path' () {
        given:
            String stubResponseBody = '''{
                                           "clients":[
                                              {
                                                 "id":echoPathVariable(user),
                                                 "surname":"Kowalsky",
                                                 "name":"Jan",
                                                 "created":"2014-02-02 12:23:43"
                                              }
                                           ],
                                           "pageMetadata":{
                                              "size":echoQueryParam(size),
                                              "totalElements":6,
                                              "totalPages":6,
                                              "number":echoQueryParam(page)
                                           }
                                        }
                                    '''
            RestAssured.port = wireMockRule.port()
            wireMockRule.stubFor(WireMock.get(urlPathMatching("/user/[0-9]+/details"))
                                .withQueryParam('page', matching('[0-9]+'))
                                .withQueryParam('size', matching('[0-9]+'))
                                .willReturn(aResponse()
                                            .withStatus(200)
                                            .withHeader("Content-Type", "application/json")
                                            .withBody(stubResponseBody)))
        expect:
            given().queryParameters(['page':'6', 'size':'1'])
                   .get('/user/12345/details')
                   .then()
                       .statusCode(200)
                       .contentType('application/json')
                       .body('clients[0].id', equalTo(12345))
                       .body('pageMetadata.size', equalTo(1))
                       .body('pageMetadata.number', equalTo(6))
    }

    def 'should return response with data from path and request body' () {
        given:
            String stubResponseBody = '''{
                                       "id":echoPathVariable(user),
                                       "surname":"Kowalsky",
                                       "name":"Jan",
                                       "addresses":[
                                          {
                                             "declared":{
                                                "city":"echoBodyElement(addresses[0].declared.city)",
                                                "street":"echoBodyElement(addresses[0].declared.street)",
                                                "number":echoBodyElement(addresses[0].declared.number)
                                             }
                                          }
                                       ],
                                       "created":"2014-02-02 12:23:43"
                                    }
                                    '''
            String requestBody = '''{
                                       "addresses":[
                                          {
                                             "declared":{
                                                "city":"Warsaw",
                                                "street":"Okopowa",
                                                "number":30
                                             }
                                          }
                                       ]
                                    }
                                    '''
            RestAssured.port = wireMockRule.port()
            wireMockRule.stubFor(WireMock.patch(urlPathMatching("/user/[0-9]+/address"))
                        .withRequestBody(equalToJson(requestBody))
                        .willReturn(aResponse()
                                    .withHeader("Content-Type", "application/json")
                                    .withStatus(200)
                                    .withBody(stubResponseBody)))
        expect:
            given().body(requestBody)
                   .patch('/user/666/address')
                   .then()
                        .statusCode(200)
                        .contentType('application/json')
                        .body('id', equalTo(666))
                        .body('addresses[0].declared.city', equalTo('Warsaw'))
                        .body('addresses[0].declared.street', equalTo('Okopowa'))
                        .body('addresses[0].declared.number', equalTo(30))

    }
}
