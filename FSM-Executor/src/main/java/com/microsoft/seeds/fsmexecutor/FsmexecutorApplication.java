package com.microsoft.seeds.fsmexecutor;

import com.microsoft.seeds.fsmexecutor.controller.FSMExecutorController;
import com.microsoft.seeds.fsmexecutor.models.fsm.FSMAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.actions.AutoEventDispatchAction;
import com.microsoft.seeds.fsmexecutor.models.fsm.actions.FSMActionList;
import com.microsoft.seeds.fsmexecutor.models.fsm.actions.SkipAction;
import com.microsoft.seeds.fsmexecutor.models.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.yaml.snakeyaml.scanner.Constant;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EnableJms
@SpringBootApplication
public class FsmexecutorApplication {
	public static ConfigurableApplicationContext applicationContext;
	private static Logger logger = Logger.getLogger(FsmexecutorApplication.class.getName());
	public static void main(String[] args) {
		applicationContext = SpringApplication.run(FsmexecutorApplication.class, args);
		JmsTemplate jmsTemplate = applicationContext.getBean(JmsTemplate.class);
		CachingConnectionFactory connectionFactory = (CachingConnectionFactory) jmsTemplate.getConnectionFactory();
		assert connectionFactory != null;
		connectionFactory.setCacheProducers(false);
		logger.info( " ********************** FSMEXECUTOR VERSION: "  + Constants.FSM_EXECUTOR_VERSION + " " + Constants.VERSION_DESCRIPTION +"  **********************");
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

	@Bean
	public WebClient.Builder getWebClientBuilder(){
		return WebClient.builder();
	}

}
