/**
 * Copyright 2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package com.netflix.conductor.core.execution.tasks;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.Task.Status;
import com.netflix.conductor.common.metadata.tasks.TaskDef;
import com.netflix.conductor.common.metadata.workflow.WorkflowTask;
import com.netflix.conductor.common.run.Workflow;
import com.netflix.conductor.core.events.ScriptEvaluator;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.core.utils.IDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.util.*;

/**
 * @author Manan
 *
 */
public class DoWhile extends WorkflowSystemTask {

	public DoWhile() {
		super("DO_WHILE");
	}

	Logger logger = LoggerFactory.getLogger(DoWhile.class);

	@Override
	public void cancel(Workflow workflow, Task task, WorkflowExecutor executor) {
		task.setStatus(Status.CANCELED);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean execute(Workflow workflow, Task task, WorkflowExecutor provider) {
		
		boolean allDone = true;
		boolean hasFailures = false;
		boolean willBeRetried = false;
		StringBuilder failureReason = new StringBuilder();
		List<WorkflowTask> loopOver = task.getWorkflowTask().getLoopOver();

		for (WorkflowTask workflowTask : loopOver){
			String refName = getTaskRefName(workflowTask, task);
			Task loopOverTask = workflow.getTaskByRefName(refName);
			if (loopOverTask == null){
				//Task is not even scheduled yet
				allDone = false;
				break;
			}
			Status taskStatus = loopOverTask.getStatus();
			hasFailures = !taskStatus.isSuccessful();
			if (hasFailures){
				failureReason.append(loopOverTask.getReasonForIncompletion()).append(" ");
			}
			task.getOutputData().put(workflowTask.getTaskReferenceName(), loopOverTask.getOutputData());
			willBeRetried = getTaskRetried(loopOverTask);
			allDone = taskStatus.isTerminal() && !willBeRetried;
			if (!allDone || hasFailures){
				break;
			}
		}
		if (hasFailures) {
			return markLoopTaskFailed(task, failureReason.toString());
		} else if (!allDone) {
			return false;
		}
		try {
			boolean shouldContinue = getEvaluatedCondition(task, workflow);
			logger.debug("taskid {} condition evaluated to {}", task.getTaskId(), shouldContinue);
			if (shouldContinue) {
				return scheduleLoopTasks(task, workflow, provider);
			} else {
				return markLoopTaskSuccess(task);
			}
		} catch (ScriptException e) {
			String message = String.format("Unable to evaluate condition {} ", task.getWorkflowTask().getLoopCondition());
			logger.error(message);
			logger.error("Marking task {} failed.", task.getTaskId());
			return markLoopTaskFailed(task, message);
		}
	}

	private boolean getTaskRetried(Task task) {
		Optional<TaskDef> taskDef = task.getTaskDefinition();
		boolean retry = false;
		if (taskDef.isPresent() && task.getRetryCount() < taskDef.get().getRetryCount()) {
			retry = true;
		}
		return retry;
	}

	private String getTaskRefName(WorkflowTask workflowTask, Task task) {
		String refName = workflowTask.getTaskReferenceName();
		if (task.getIteration()>0) {
			refName +=  "_" + task.getIteration();
		}
		return refName;
	}

	Map<String, Object> getConditionInput(Task task, Workflow workflow) {
		Map<String, Object> map = new HashMap<>();
		List<WorkflowTask> loopOverTask = task.getWorkflowTask().getLoopOver();
		for (WorkflowTask workflowTask : loopOverTask) {
			Task temp = workflow.getTaskByRefName(getTaskRefName(workflowTask, task));
			map.put(workflowTask.getTaskReferenceName(), temp.getOutputData());
			logger.debug("Taskname {} output {}", temp.getReferenceTaskName(), temp.getOutputData());
		}
		return map;
	}

	boolean scheduleLoopTasks(Task task, Workflow workflow, WorkflowExecutor provider) {
		logger.debug("Scheduling loop tasks for taskid {} as condition {} evaluated to true",
				task.getTaskId(), task.getWorkflowTask().getLoopCondition());
		List<WorkflowTask> loopOver = task.getWorkflowTask().getLoopOver();
		List<Task> taskToBeScheduled = new ArrayList<>();
		int iteration = task.getIteration() + 1;
		for (WorkflowTask loopOverRef : loopOver){
			String refName = getTaskRefName(loopOverRef, task);
			Task existingTask = workflow.getTaskByRefName(refName);
			Task newTask = existingTask.copy();
			newTask.setTaskId(IDGenerator.generate());
			newTask.setStatus(Status.SCHEDULED);
			newTask.setReferenceTaskName(loopOverRef.getTaskReferenceName() + "_" + iteration);
			newTask.setRetryCount(existingTask.getRetryCount());
			newTask.setScheduledTime(System.currentTimeMillis());
			taskToBeScheduled.add(newTask);
		}
		task.setIteration(iteration);
		provider.scheduleTask(workflow, taskToBeScheduled);
		return true; // Return true even though status not changed. Iteration has to be updated in execution DAO.
	}

	boolean markLoopTaskFailed(Task task, String failureReason) {
		task.setReasonForIncompletion(failureReason);
		task.setStatus(Status.FAILED);
		logger.debug("taskid {} failed in {} iteration",task.getTaskId(), task.getIteration() + 1);
		return true;
	}

	boolean markLoopTaskSuccess(Task task) {
		logger.debug("taskid {} took {} iterations to complete",task.getTaskId(), task.getIteration() + 1);
		task.setStatus(Status.COMPLETED);
		return true;
	}

	@VisibleForTesting
	boolean getEvaluatedCondition(Task task, Workflow workflow) throws ScriptException {
		Map<String, Object> taskInput = getConditionInput(task, workflow);
		String condition = task.getWorkflowTask().getLoopCondition();
		boolean shouldContinue = false;
		if (condition != null) {
			logger.debug("Condition {} is being evaluated{}", condition);
			//Evaluate the expression by using the Nashhorn based script evaluator
			shouldContinue = ScriptEvaluator.evalBool(condition, taskInput);
		}
		return shouldContinue;
	}

}
