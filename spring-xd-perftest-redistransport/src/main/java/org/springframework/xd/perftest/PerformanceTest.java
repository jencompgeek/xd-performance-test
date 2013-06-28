/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.xd.perftest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Ilayaperumal Gopinathan
 *
 */
public class PerformanceTest {
	
	private static final Log logger = LogFactory.getLog(PerformanceTest.class);
	
	private static Transport transport;
	
	private static ConsumerType consumerType;
	
	public static void main(String[] args) {
		updatePerfTestHome();
		PerformanceTest perfTest = new PerformanceTest();
		CmdLineOptions options = perfTest.new CmdLineOptions();
		CmdLineParser parser = new CmdLineParser(options);
		parser.setUsageWidth(90);
		try {
			parser.parseArgument(args);
		} 
		catch (CmdLineException e) {
			parser.printUsage(System.err);
			System.exit(1);
		}
		transport = options.getTransport();
		consumerType = options.getConsumerType();
		setConnectionFactoryType(options.getConnectionFactoryType());
		if (options.isShowHelp()) {
			parser.printUsage(System.err);
			System.exit(0);
		}
		logger.info("*******Starting Performance test********");
		logger.info("Transport: "+ transport);
		logger.info("Consumer:  "+ consumerType);
		logger.info("ConnectionFactory: "+options.getConnectionFactoryType());
		logger.info("****************************************");
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext();
		context.getEnvironment().setActiveProfiles(consumerType.name());
		context.setConfigLocation("META-INF/perftest/"+ transport +"-transport-perftest.xml");
		context.refresh();
		context.registerShutdownHook();

	}
	
	private static void updatePerfTestHome() {
		System.setProperty("perftest.home", System.getProperty("perftest.home", ".."));
	}
	
	private static void setConnectionFactoryType(ConnectionFactoryType connectionFactoryType) {
		System.setProperty("redis.connectionFactory", 
				(connectionFactoryType.name().equals("noPool") ? "default" : "pool"));
		System.getProperty("redis.connectionFactory");
	}
	
	// Transport Enum 
	private enum Transport {
		redis
	}
	
	// Consumer type Enum
	private enum ConsumerType {
		concurrent,
		scheduler
	}
	
	//ConnectionFactory type
	private enum ConnectionFactoryType {
		noPool,
		pool
	}

	private class CmdLineOptions {
				
		@Option(name = "--help", usage = "Show options help", aliases = { "-?",	"-h" })
		private boolean showHelp = false;

		@Option(name = "--transport", usage = "The transport to be used (default: redis)")
		private Transport transport = Transport.redis;

		@Option(name = "--consumer", usage = "Consumer type (default: concurrent)")
		private ConsumerType consumerType = ConsumerType.concurrent;
		
		@Option(name = "--connectionFactory", usage = "ConnectionFactory type (default: noPool)")
		private ConnectionFactoryType connectionFactoryType = ConnectionFactoryType.noPool;
		
		/**
		 * @return transport
		 */
		public Transport getTransport() {
			return transport;
		}
		
		/**
		 * @return consumerType
		 */
		public ConsumerType getConsumerType() {
			return consumerType;
		}
		
		/**
		 * @return connectionFactoryType
		 */
		public ConnectionFactoryType getConnectionFactoryType() {
			return connectionFactoryType;
		}
		
		/**
		 * @return the showHelp
		 */
		public boolean isShowHelp() {
			return showHelp;
		}
	}
}
