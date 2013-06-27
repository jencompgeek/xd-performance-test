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
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Ilayaperumal Gopinathan
 */
public class RedisQInboundChannelAdapter extends MessageProducerSupport {

	private final String queueName;

	private volatile boolean extractPayload = true;

	private final StringRedisTemplate redisTemplate = new StringRedisTemplate();

	private final ObjectMapper objectMapper = new ObjectMapper();
	
	protected volatile Boolean blockingRightPop = false;
	
	private AtomicBoolean receivingFirstMsg = new AtomicBoolean(false);
	
	private AtomicLong msgCounter = new AtomicLong(0L);
	
	private Timer timer = new Timer();


	public RedisQInboundChannelAdapter(String queueName, RedisConnectionFactory connectionFactory) {
		Assert.hasText(queueName, "queueName is required");
		Assert.notNull(connectionFactory, "connectionFactory must not be null");
		this.queueName = queueName;
		this.redisTemplate.setConnectionFactory(connectionFactory);
		this.redisTemplate.afterPropertiesSet();
	}


	public void setExtractPayload(boolean extractPayload) {
		this.extractPayload = extractPayload;
	}
	
	public void setBlockingRightPop(boolean blockingRightPop) {
		this.blockingRightPop = blockingRightPop;
	}
	
	public boolean isBlockingRightPop(){
		return this.blockingRightPop;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.setupCounter();
	}

	@Override
	protected void doStart() {
		super.doStart();
	}

	@Override
	protected void doStop() {
		super.doStop();
	}

	protected void setupCounter() {
		TimerTask task = new TimerTask() {
			public void run() {
				System.out.println(new Date(System.currentTimeMillis())+ " Received " +
						 + msgCounter.getAndSet(0L) + " messages in 10 seconds.");
			}
		};
		// Set period 10 seconds
		timer.scheduleAtFixedRate(task, 0L, 10000L);
	}

	protected class ListenerTask implements Runnable {

		@Override
		public void run() {
			try {
				while (isRunning()) {
					String next = (isBlockingRightPop() ? redisTemplate.boundListOps(queueName).rightPop(5, TimeUnit.SECONDS)
							: redisTemplate.boundListOps(queueName).rightPop());
					if (next != null) {
						if (receivingFirstMsg.compareAndSet(false, true)) {
							System.out.println("Started receiving messages at: "+ new Date());
						}
						try {
							Message<?> message = null;
							if (extractPayload) {
								message = MessageBuilder.withPayload(next).build();
							} else {
								MessageDeserializationWrapper wrapper = objectMapper.readValue(next,
										MessageDeserializationWrapper.class);
								message = wrapper.getMessage();
							}
							sendMessage(message);
							msgCounter.incrementAndGet();
						} catch (Exception e) {
							logger.error("Error sending message", e);
						}
					}

				}
			} catch (RedisSystemException e) {
				if(isRunning()) {
					logger.error("Error polling Redis queue", e);
				}
			}
		}
	}


	@SuppressWarnings("unused") // used by object mapper
	private static class MessageDeserializationWrapper {

		private volatile Map<String, Object> headers;

		private volatile Object payload;

		private volatile Message<?> message;

		void setHeaders(Map<String, Object> headers) {
			this.headers = headers;
		}

		void setPayload(Object payload) {
			this.payload = payload;
		}

		Message<?> getMessage() {
			if (this.message == null) {
				this.message = MessageBuilder.withPayload(this.payload).copyHeaders(this.headers).build();
			}
			return this.message;
		}
	}

}
