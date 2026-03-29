package com.thanhxv.ui.worker;

import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;

@Slf4j
public abstract class AbstractExternalTask implements ExternalTaskHandler {
    protected static final String ERROR_MSG_CAMUNDA = "error-mesage";

    protected void exceptionHandler(ExternalTaskService externalTaskService, ExternalTask externalTask, Exception exception) {
        log.error("Error processing task {}: {}", externalTask.getId(), exception.getMessage());
        externalTaskService.handleFailure(externalTask.getId(),
                ERROR_MSG_CAMUNDA,
                exception.getMessage(),
                0,
                0);
    }
}
