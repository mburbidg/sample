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

import java.util.Collection;
import java.util.Collections;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {SampleApplicationTests.Config.class})
public class SampleApplicationTests {
	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {
		@Bean
		public Driver driver() {
			return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "password"));
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(EntityNode.class.getPackage().getName());
		}

	}

	@Test
	void templateTest(@Autowired ReactiveNeo4jTemplate reactiveNeo4jTemplate) {
		reactiveNeo4jTemplate.findAll(EntityNode.class)
				.flatMap(e -> {
					System.out.printf("entityId=%s\n", e.getAssetId());
					return Mono.just(e);
				})
				.subscribe();
	}
}