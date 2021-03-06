package io.codearte.accurest.stubrunner.messaging.integration

import groovy.transform.CompileStatic
import io.codearte.accurest.dsl.GroovyDsl
import io.codearte.accurest.stubrunner.BatchStubRunner
import io.codearte.accurest.stubrunner.StubConfiguration
import io.codearte.accurest.stubrunner.spring.StubRunnerConfiguration
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.Lifecycle
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.integration.dsl.FilterEndpointSpec
import org.springframework.integration.dsl.GenericEndpointSpec
import org.springframework.integration.dsl.IntegrationFlowBuilder
import org.springframework.integration.dsl.IntegrationFlows
import org.springframework.messaging.Message

/**
 * Spring Integration configuration that iterates over the downloaded Groovy DSLs
 * and registers a flow for each DSL.
 *
 * @author Marcin Grzejszczak
 */
@Configuration
@Import(StubRunnerConfiguration)
@CompileStatic
class StubRunnerIntegrationConfiguration {

	@Bean
	FlowRegistrar flowRegistrar(AutowireCapableBeanFactory beanFactory, BatchStubRunner batchStubRunner) {
		Map<StubConfiguration, Collection<GroovyDsl>> accurestContracts = batchStubRunner.accurestContracts
		accurestContracts.each { StubConfiguration key, Collection<GroovyDsl> value ->
			String name = "${key.groupId}_${key.artifactId}"
			value.findAll { it?.input?.messageFrom?.clientValue }.each { GroovyDsl dsl ->
				String flowName = "${name}_${dsl.label}_${dsl.hashCode()}"
				IntegrationFlowBuilder builder = IntegrationFlows.from(dsl.input.messageFrom.clientValue)
					.filter(new StubRunnerIntegrationMessageSelector(dsl), { FilterEndpointSpec e -> e.id("${flowName}.filter") } )
					.transform(new StubRunnerIntegrationTransformer(dsl), { GenericEndpointSpec e -> e.id("${flowName}.transformer") })
				if (dsl.outputMessage?.sentTo) {
					builder = builder.channel(dsl.outputMessage.sentTo.clientValue)
				} else {
					builder = builder.handle(new DummyMessageHandler(), "handle")
				}
				beanFactory.initializeBean(builder.get(), flowName)
				beanFactory.getBean("${flowName}.filter", Lifecycle.class).start();
				beanFactory.getBean("${flowName}.transformer", Lifecycle.class).start();
			}
		}
		return new FlowRegistrar()
	}

	@CompileStatic
	private static class DummyMessageHandler {
		void handle(Message message) {}
	}

	static class FlowRegistrar {}
}
