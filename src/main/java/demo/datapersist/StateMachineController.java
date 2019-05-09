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

import demo.datapersist.StateMachineConfig.Events;
import demo.datapersist.StateMachineConfig.States;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachinePersist;
import org.springframework.statemachine.StateMachineSystemConstants;
import org.springframework.statemachine.annotation.WithStateMachine;
import org.springframework.statemachine.region.Region;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.state.AbstractState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Controller;

@Controller
@WithStateMachine
public class StateMachineController {

	private static final String MACHINE_ID = "datajpapersist";

	private StateMachine<States, Events> currentStateMachine;

    @Qualifier(StateMachineSystemConstants.TASK_EXECUTOR_BEAN_NAME)
    @Autowired
    public TaskExecutor taskExecutor;

	@Autowired
	private StateMachineService<States, Events> stateMachineService;

	@Autowired
	private StateMachinePersist<States, Events, String> stateMachinePersist;

	private boolean isKilled = false;

	public void init() {
	    if(isKilled) {
            ((ThreadPoolTaskExecutor)taskExecutor).initialize();
        }

		currentStateMachine = stateMachineService.acquireStateMachine(MACHINE_ID);
        AbstractState state = (AbstractState)currentStateMachine.getState();
        System.out.println(String.format("Started state machine with state '%s'", state.getId()));

        final StringBuilder builder = new StringBuilder();
        if(state.getRegions().isEmpty()) {
            builder.append("No subregions");
        } else {
            state.getRegions().stream().forEach(e -> {
                builder.append(" ");
                builder.append(((Region)e).getState().getId());
            });
        }
        System.out.println(String.format("Subregions :'%s'", builder.toString()));


        currentStateMachine.start();
		currentStateMachine.sendEvent(Events.START);
	}

	public void killAll() {
        System.out.println("Killing state machine threads");
        ((ThreadPoolTaskExecutor)taskExecutor).shutdown();
        isKilled = true;
    }
}
