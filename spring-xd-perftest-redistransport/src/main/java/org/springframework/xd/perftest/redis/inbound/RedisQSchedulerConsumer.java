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

import java.util.Date;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class RedisQSchedulerConsumer extends RedisQInboundChannelAdapter {
	
	private static final Log logger = LogFactory.getLog(RedisQSchedulerConsumer.class);

	private volatile TaskScheduler taskScheduler;
	
	private volatile ScheduledFuture<?> listenerTask;


	public RedisQSchedulerConsumer(String queueName, RedisConnectionFactory connectionFactory) {
		super(queueName, connectionFactory);
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.taskScheduler = this.getTaskScheduler();
		if (this.taskScheduler == null) {
			ThreadPoolTaskScheduler tpts = new ThreadPoolTaskScheduler();
			tpts.afterPropertiesSet();
			this.taskScheduler = tpts;
		}
		logger.info("Initialize RedisQInboundChannelAdapter with taskScheduler");
	}
	
	@Override
	protected void doStart() {
		super.doStart();
		this.listenerTask = this.taskScheduler.schedule(new ListenerTask(), new Date());
	}

	@Override
	protected void doStop() {
		super.doStop();
		if (this.listenerTask != null) {
			this.listenerTask.cancel(true);
		}
	}

}
