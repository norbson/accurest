package io.codearte.accurest.stubrunner.extension

import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.extension.Parameters
import com.github.tomakehurst.wiremock.extension.ResponseDefinitionTransformer
import com.github.tomakehurst.wiremock.http.Request
import com.github.tomakehurst.wiremock.http.ResponseDefinition
import com.jayway.jsonpath.JsonPath

import java.util.regex.Matcher
import java.util.regex.Pattern

import static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.like
import static org.apache.commons.lang3.StringUtils.isBlank
import static org.apache.commons.lang3.StringUtils.isNotBlank

/**
 * @author Norbert Krzysztofik
 */
class EchoRequestTransformer extends ResponseDefinitionTransformer {

    private final Pattern echoBodyElementTagPattern =  ~/echoBodyElement\((.+?)\)/
    private final Pattern echoPathVariableTagPattern =  ~/echoPathVariable\((.+?)\)/
    private final Pattern echoQueryParamTagPattern =  ~/echoQueryParam\((.+?)\)/

    @Override
    ResponseDefinition transform(Request request, ResponseDefinition responseDefinition,
                                 FileSource files, Parameters parameters) {
        String responseBody = responseDefinition.getBody()

        if (isBlank(responseBody)) {
            return responseDefinition;
        }

        return like(responseDefinition)
                .but()
                .withBody(transformResponse(request, responseBody))
                .build()
    }

    @Override
    String name() {
        'echo-request-property'
    }

    private String transformResponse(Request request, String responseBody) {
        String modifiedResponseBody = responseBody
        String jsonRequestBody = request.getBodyAsString()
        if (isNotBlank(jsonRequestBody)) {
            modifiedResponseBody = echoFromRequestBody(modifiedResponseBody, jsonRequestBody)
        }
        modifiedResponseBody = echoFromRequestUrl(modifiedResponseBody, request)
        return modifiedResponseBody
    }

    private String echoFromRequestUrl(String responseBody, Request request) {
        String modifiedResponseBody = responseBody
        modifiedResponseBody = echoPathVariable(modifiedResponseBody, request.getUrl())
        modifiedResponseBody = echoQueryParam(modifiedResponseBody, request)
        return modifiedResponseBody
    }

    private String echoFromRequestBody(String responseBody, String jsonRequestBody) {
        String modifiedResponseBody = responseBody
        Matcher echoBodyElementTagFinder = (modifiedResponseBody =~ echoBodyElementTagPattern)
        echoBodyElementTagFinder.findAll() { String echoBodyElementTag, String pathToElement ->
            modifiedResponseBody = modifiedResponseBody.replace(echoBodyElementTag,
                                                                getBodyElementValueFromRequestBody(pathToElement, jsonRequestBody))
        }
        return modifiedResponseBody
    }

    private String echoQueryParam(String responseBody, Request request) {
        String modifiedResponseBody = responseBody
        Matcher echoQueryParamTagFinder = (modifiedResponseBody =~ echoQueryParamTagPattern)
        echoQueryParamTagFinder.findAll() { String echoQueryParamTag, String queryParamName ->
            modifiedResponseBody = modifiedResponseBody.replace(echoQueryParamTag,
                                                                request.queryParameter(queryParamName).firstValue())
        }
        return modifiedResponseBody
    }

    private String echoPathVariable(String responseBody, String requestUrl) {
        String modifiedResponseBody = responseBody
        Matcher echoPathVariableTagFinder = (modifiedResponseBody =~ echoPathVariableTagPattern)
        echoPathVariableTagFinder.findAll() { String echoPathVariableTag, String pathVariableName ->
            modifiedResponseBody = modifiedResponseBody.replace(echoPathVariableTag,
                                                                getPathVariableValueFromRequestUrl(pathVariableName, requestUrl))
        }
        return modifiedResponseBody
    }

    private static String getBodyElementValueFromRequestBody(String pathToElement, String jsonRequestBody) {
        return JsonPath.read(jsonRequestBody, "\$.${pathToElement}") as String
    }

    private static String getPathVariableValueFromRequestUrl(String pathVariableName, String requestUrl) {
        return requestUrl.find(/${pathVariableName}\/([^\/\?]+?)(\/|$|\?)/) { it[1] }
    }
}
