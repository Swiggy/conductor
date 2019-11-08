package com.netflix.conductor.kafka;

import com.google.inject.AbstractModule;
import com.netflix.conductor.core.config.SystemPropertiesConfiguration;
import com.netflix.conductor.dao.IndexDAO;
import com.netflix.conductor.dao.ProducerDAO;
import com.netflix.conductor.dao.kafka.index.KafkaDAO;
import com.netflix.conductor.dao.kafka.index.producer.KafkaProducer;
import com.netflix.conductor.elasticsearch.EmbeddedElasticSearchProvider;
import com.netflix.conductor.elasticsearch.es5.EmbeddedElasticSearchV5Provider;


public class KafkaModule extends AbstractModule {

    public KafkaModule() {
    }

    @Override
    protected void configure() {
        SystemPropertiesConfiguration configuration = new SystemPropertiesConfiguration();
        if (configuration.getKafkaIndexEnable()) {
            bind(ProducerDAO.class).to(KafkaProducer.class);
            bind(IndexDAO.class).to(KafkaDAO.class);
            bind(EmbeddedElasticSearchProvider.class).to(EmbeddedElasticSearchV5Provider.class);
        }
    }
}

