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
package org.springframework.xd.perftest.redis.inbound;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * @author Ilayaperumal Gopinathan
 *
 */
public class RedisQConcurrentConsumer extends RedisQInboundChannelAdapter {
	
	private volatile int numListenerThreads = 1;
	
	private volatile ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
	
	public RedisQConcurrentConsumer(String queueName, RedisConnectionFactory connectionFactory) {
		super(queueName, connectionFactory);
	}
	
	public void setNumListenerThreads(int numListenerThreads) {
		this.numListenerThreads = numListenerThreads;
	}
	
	@Override
	protected void onInit() {
		super.onInit();
		this.taskExecutor.setCorePoolSize(numListenerThreads);
		this.taskExecutor.setMaxPoolSize(numListenerThreads);
		this.taskExecutor.setBeanName("Redis-Performance-Test");
		this.taskExecutor.initialize();
	}
	
	@Override
	protected void doStart(){
		super.doStart();
		for (int i = 0; i < this.taskExecutor.getCorePoolSize(); i++) {
			this.taskExecutor.execute(new RedisQInboundChannelAdapter.ListenerTask());
		}
	}

}
