/*
 * Copyright 2017-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package demo.datapersist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.StateMachineSystemConstants;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.data.jpa.JpaPersistingStateMachineInterceptor;
import org.springframework.statemachine.data.jpa.JpaStateMachineRepository;
import org.springframework.statemachine.persist.StateMachineRuntimePersister;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

@Slf4j
@Configuration
@EnableStateMachineFactory
public class StateMachineConfig
		extends EnumStateMachineConfigurerAdapter<StateMachineConfig.States, StateMachineConfig.Events> {

	@Autowired
	private StateMachineRuntimePersister<States, Events, String> stateMachineRuntimePersister;

	@Override
	public void configure(StateMachineStateConfigurer<States, Events> states)
			throws Exception {

		states.withStates()
				.initial(States.NOT_STARTED)
				.fork(States.FORK)
				.state(States.TASKS)
				.join(States.JOIN)
				.and().withStates().parent(States.TASKS)
				.initial(States.TASK1)
				.state(States.TASK1_WORK)
				.end(States.TASK1_DONE)
				.and().withStates().parent(States.TASKS)
				.initial(States.TASK2)
				.state(States.TASK2_WORK)
				.end(States.TASK2_DONE)
				.and().withStates().state(States.END)
				.end(States.END);
	}

	@Override
	public void configure(StateMachineTransitionConfigurer<States, Events> transitions)
			throws Exception {
		transitions
				.withExternal()
				.source(States.NOT_STARTED).target(States.FORK)
				.event(Events.START)
				.and()
				.withFork()
				.source(States.FORK).target(States.TASKS)

				.and().withExternal().source(States.TASK1).target(States.TASK1_WORK).action(Util::process)
				.and().withExternal().source(States.TASK1_WORK).target(States.TASK1_DONE).action(Util::process)

				.and().withExternal().source(States.TASK2).target(States.TASK2_WORK).action(Util::process)
				.and().withExternal().source(States.TASK2_WORK).target(States.TASK2_DONE).action(Util::process)

				.and()
				.withJoin().source(States.TASKS).target(States.JOIN)
				.and().withExternal().source(States.JOIN).target(States.END);
	}

	@Bean(name = StateMachineSystemConstants.TASK_EXECUTOR_BEAN_NAME)
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setWaitForTasksToCompleteOnShutdown(false);
		taskExecutor.setCorePoolSize(5);
		return taskExecutor;
	}

	@Bean
	public StateMachineService<States, Events> stateMachineService(
			StateMachineFactory<States, Events> stateMachineFactory,
			StateMachineRuntimePersister<States, Events, String> stateMachineRuntimePersister) {
		return new DefaultStateMachineService<>(stateMachineFactory, stateMachineRuntimePersister);
	}


	@Override
	public void configure(StateMachineConfigurationConfigurer<States, Events> config)
			throws Exception {
		config
				.withConfiguration()
				.and()
				.withPersistence()
				.runtimePersister(stateMachineRuntimePersister);
	}

	@Bean
	public StateMachineRuntimePersister<StateMachineConfig.States, StateMachineConfig.Events, String> stateMachineRuntimePersister(
			JpaStateMachineRepository jpaStateMachineRepository) {

		return new JpaPersistingStateMachineInterceptor<StateMachineConfig.States, StateMachineConfig.Events, String>(jpaStateMachineRepository) {
			@Override
			public void write(StateMachineContext<StateMachineConfig.States, StateMachineConfig.Events> context, String  contextObj) throws Exception {
				System.out.println(String.format("Saving %s - %s", contextObj, context.getState().name()));
				super.write(context, contextObj);
			}

		};
	}

	public enum States {
		NOT_STARTED, FORK, TASKS, JOIN,
		TASK1, TASK1_WORK, TASK1_DONE,
		TASK2, TASK2_WORK, TASK2_DONE,
		END
	}

	public enum Events {
		START
	}
}
