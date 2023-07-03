package com.microsoft.seeds.place;

import com.microsoft.seeds.place.models.utils.Constants;
import org.json.JSONObject;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
@EnableJms
public class PlaceApplication {
	public static ConfigurableApplicationContext applicationContext;


	public static void main(String[] args) {
		applicationContext = SpringApplication.run(PlaceApplication.class, args);
		JmsTemplate jmsTemplate = applicationContext.getBean(JmsTemplate.class);
		CachingConnectionFactory connectionFactory = (CachingConnectionFactory) jmsTemplate.getConnectionFactory();
		connectionFactory.setCacheProducers(false);
		System.out.println("**************************** PLACE VERSION : " + Constants.VERSION_PLACE + " ------ " + Constants.VERSION_DESCRIPTION +" ****************************");
	}

	@Bean
	public WebClient.Builder getWebClientBuilder(){
		return WebClient.builder();
	}

}
