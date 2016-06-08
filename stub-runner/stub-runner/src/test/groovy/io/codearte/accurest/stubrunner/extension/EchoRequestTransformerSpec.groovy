package io.codearte.accurest.stubrunner.extension

import com.github.tomakehurst.wiremock.http.QueryParameter
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import io.codearte.accurest.util.AssertionUtil
import spock.lang.Specification
import spock.lang.Subject


/**
 * @author Norbert Krzysztofik
 */
class EchoRequestTransformerSpec extends Specification {

    Request request = Stub()
    ResponseDefinition responseDefinition = Stub()

    @Subject
    EchoRequestTransformer echoRequestTransformer = new EchoRequestTransformer()

    def setup() {
        responseDefinition.getByteBodyIfBinary() >> null
    }


    def 'shouldResponseWithUserIdFromRequestUrl'() {
        given:
        String url = '/get/user/12345/details'
        String responseBody = '''
                                    {
                                       "id":"echoPathVariable(user)",
                                       "surname":"Kowalsky",
                                       "name":"Jan",
                                       "created":"2014-02-02 12:23:43"
                                    }
                                    '''

        request.getUrl() >> url
        responseDefinition.getBody() >> responseBody

        when:
        ResponseDefinition transformedResponse = echoRequestTransformer.transform(request,
                                                                                  responseDefinition, null, null)
        then:
        AssertionUtil.assertThatJsonsAreEqual('''
                                                    {
                                                       "id":"12345",
                                                       "surname":"Kowalsky",
                                                       "name":"Jan",
                                                       "created":"2014-02-02 12:23:43"
                                                    }
                                                    ''', transformedResponse.getBody())
    }

    def 'shouldResponseWithPageNumberAndSizeFromRequestQueryParam'() {
        given:
            String url = '/get/user/12345/details?page=6&size=1'
            String responseBody = '''{
                                       "clients":[
                                          {
                                             "id":"12345",
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
            request.getUrl() >> url
            request.queryParameter('page') >> new QueryParameter('page', ['6'])
            request.queryParameter('size') >> new QueryParameter('size', ['1'])
            responseDefinition.getBody() >> responseBody

        when:
            ResponseDefinition transformedResponse = echoRequestTransformer.transform(request,
                                                                                      responseDefinition, null, null)
        then:
            AssertionUtil.assertThatJsonsAreEqual('''{
                                                       "clients":[
                                                          {
                                                             "id":"12345",
                                                             "surname":"Kowalsky",
                                                             "name":"Jan",
                                                             "created":"2014-02-02 12:23:43"
                                                          }
                                                       ],
                                                       "pageMetadata":{
                                                          "size":1,
                                                          "totalElements":6,
                                                          "totalPages":6,
                                                          "number":6
                                                       }
                                                    }
                                                ''', transformedResponse.getBody())
    }

    def 'shouldResponseWithDataTakenFromRequestBody'() {
        given:
            String url = '/patch/user/12345/address'
            String requestBody = '''{
                                     "city":"Warsaw",
                                     "street":"Okopowa",
                                     "number":30
                                    }
                                '''
            String responseBody = '''{
                                        "id":"12345",
                                        "surname":"Kowalsky",
                                        "name":"Jan",
                                        "address": {
                                             "city":"echoBodyElement(city)",
                                             "street":"echoBodyElement(street)",
                                             "number":echoBodyElement(number)
                                            },
                                        "created":"2014-02-02 12:23:43"
                                        }
                                    '''
            request.getUrl() >> url
            request.getBodyAsString() >> requestBody
            responseDefinition.getBody() >> responseBody

        when:
            ResponseDefinition transformedResponse = echoRequestTransformer.transform(request,
                                                                                      responseDefinition, null, null)
        then:
            AssertionUtil.assertThatJsonsAreEqual('''{
                                                        "id":"12345",
                                                        "surname":"Kowalsky",
                                                        "name":"Jan",
                                                        "address": {
                                                             "city":"Warsaw",
                                                             "street":"Okopowa",
                                                             "number":30
                                                            },
                                                        "created":"2014-02-02 12:23:43"
                                                        }
                                                    ''', transformedResponse.getBody())
    }
}
