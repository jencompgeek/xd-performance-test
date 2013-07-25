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

package org.springframework.xd.perftest.redis.outbound;

import java.util.Date;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class RedisQOutboundChannelAdapter extends AbstractMessageHandler {

	private final String queueName;

	private volatile boolean extractPayload = true;

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	private final ObjectMapper objectMapper = new ObjectMapper();
	
	private AtomicLong msgCounter = new AtomicLong(0L);
	
	private Timer timer = new Timer();

	private Queue<String> messages = new ConcurrentLinkedQueue<String>();

	private static final int BATCH_SIZE=500;

	private AtomicInteger batchCounter = new AtomicInteger();


	public RedisQOutboundChannelAdapter(String queueName, RedisConnectionFactory connectionFactory) {
		Assert.hasText(queueName, "queueName is required");
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.queueName = queueName;
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.afterPropertiesSet();
		this.setupCounter();
	}


	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String s = (this.extractPayload) ? message.getPayload().toString() : this.objectMapper.writeValueAsString(message);
		if (logger.isDebugEnabled()) {
			logger.debug("sending to redis queue '" + this.queueName + "': " + s);
		}
		batchSend(s);
	}

	private void batchSend(String s) {
		//TODO assuming OK to lose Exceptions, we need to catch and log. Also, we need a time gate to ensure we
		//are picking up messages if less than batch size
		batchCounter.incrementAndGet();
		messages.offer(s);
		if(batchCounter.get() == BATCH_SIZE) {
			batchCounter.set(0);
			redisTemplate.execute(new RedisCallback<Object>() {
				@Override
				public Object doInRedis(RedisConnection connection) throws DataAccessException {
					for(int i=0;i<BATCH_SIZE;i++) {
						((StringRedisConnection)connection).lPush(queueName, messages.remove());
						msgCounter.incrementAndGet();
					}
					return null;
				}
			}, false, true);
		}
	}

	private void send(String s) {
		redisTemplate.boundListOps(queueName).leftPush(s);
		msgCounter.incrementAndGet();
	}
	
	private void setupCounter() {
		TimerTask task = new TimerTask() {
			public void run() {
				System.out.println(new Date(System.currentTimeMillis())+ " Sending " +
						 + msgCounter.getAndSet(0L) + " messages in 10 seconds.");
			}
		};
		// Set period 10 seconds
		timer.scheduleAtFixedRate(task, 0L, 10000L);
	}

}
