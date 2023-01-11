package com.adobe.cos.model.composite.sample;

import com.adobe.cos.model.composite.EntityNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SampleApplicationTests.Config.class})
public class SampleApplicationTests {
	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {
		@Bean
		public Driver driver() {
			return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "secret"));
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(EntityNode.class.getPackage().getName());
		}

	}

	@Test
	void templateTest(@Autowired ReactiveNeo4jTemplate reactiveNeo4jTemplate) {

		// The issue with this code is that although you subscribe to the publisher, it doesn't wait for the publisher to
		// finish. That means, after `subscribe` the test class will continue to run and as there is nothing more in the
		// end, the test finishes and Spring infrastructure will clean up the beans and with them, the resources or in other
		// words, it will close the driver.
		// Let's dissect this:

		/*
		reactor.core.Exceptions$ErrorCallbackNotImplemented: java.lang.RuntimeException: Async resource cleanup failed after onError
Caused by: java.lang.RuntimeException: Async resource cleanup failed after onError
	at reactor.core.publisher.FluxUsingWhen$RollbackInner.onError(FluxUsingWhen.java:468) ~[reactor-core-3.4.26.jar:3.4.26]
	at reactor.core.publisher.FluxPeek$PeekSubscriber.onError(FluxPeek.java:222) ~[reactor-core-3.4.26.jar:3.4.26]
	[…] omittied because irrelevant
	Suppressed: org.neo4j.driver.exceptions.ServiceUnavailableException: Connection pool for server localhost:7687 is closed while acquiring a connection.
Caused by: org.neo4j.driver.exceptions.ServiceUnavailableException: Connection pool for server localhost:7687 is closed while acquiring a connection.
	at org.neo4j.driver.internal.async.pool.ConnectionPoolImpl.processAcquisitionError(ConnectionPoolImpl.java:229) ~[neo4j-java-driver-4.4.11.jar:4.4.11-7d3fdc18543dae49c0c337b2885771b4f38a288d]
	at org.neo4j.driver.internal.async.pool.ConnectionPoolImpl.lambda$acquire$0(ConnectionPoolImpl.java:130) ~[neo4j-java-driver-4.4.11.jar:4.4.11-7d3fdc18543dae49c0c337b2885771b4f38a288d]
	at java.base/java.util.concurrent.CompletableFuture.uniHandle(CompletableFuture.java:934) ~[na:na]
	at java.base/java.util.concurrent.CompletableFuture$UniHandle.tryFire(CompletableFuture.java:911) ~[na:na]
	... 15 common frames omitted
Caused by: java.lang.IllegalStateException: FixedChannelPool was closed
	at org.neo4j.driver.internal.shaded.io.netty.channel.pool.FixedChannelPool$AcquireListener.operationComplete(FixedChannelPool.java:419) ~[neo4j-java-driver-4.4.11.jar:4.4.11-7d3fdc18543dae49c0c337b2885771b4f38a288d]
	... 12 common frames omitted
		 */

		// First: The flow errors out, the NPE: I don't know, I don't care, reactor internal
		// First cause: The reactive flow noticed it errored out, it tries to close the resources which are already closed
		// 2nd cause: This is the error that actually caused the flow to fail: "Connection pool for server localhost:7687 is closed while acquiring a connection."
		//            The flow started to run while the test was about to end and already closed the connect
		// 3rd cause: Driver internal

		reactiveNeo4jTemplate.findAll(EntityNode.class)
				.flatMap(e -> {
					System.out.printf("entityId=%s\n", e.getAssetId());
					return Mono.just(e);
				})
				.subscribe();
	}


	// Naive solution that leaves your code intact
	@Test
	void templateTestNaiveSolution(@Autowired ReactiveNeo4jTemplate reactiveNeo4jTemplate) throws InterruptedException {

		var latch = new CountDownLatch(1);
		reactiveNeo4jTemplate.findAll(EntityNode.class)
			.flatMap(e -> {
				System.out.printf("entityId=%s\n", e.getAssetId());
				return Mono.just(e);
			})
			.doOnComplete(latch::countDown)
			.subscribe();
		latch.await();
	}

	// Naive solution that leaves your code intact
	@Test
	void templateTestProperSolution(@Autowired ReactiveNeo4jTemplate reactiveNeo4jTemplate) throws InterruptedException {

		reactiveNeo4jTemplate.findAll(EntityNode.class)
			.flatMap(e -> {
				System.out.printf("entityId=%s\n", e.getAssetId());
				return Mono.empty();
			})
			.as(StepVerifier::create)
			.verifyComplete();

		/*
		// If you know your dataset, you will want to verify it like this
		reactiveNeo4jTemplate.findAll(EntityNode.class)
			.as(StepVerifier::create)
			// .expectNextMatches(node -> node.getAssetId().equals("foobar")) // for checking individual elements
			.expectNextCount(1L) // Or the total number
			.verifyComplete();
		 */
	}
}