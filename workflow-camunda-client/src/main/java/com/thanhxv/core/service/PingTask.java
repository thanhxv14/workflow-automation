package com.thanhxv.core.service;

import com.thanhxv.ui.worker.AbstractExternalTask;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;

@Component
@ExternalTaskSubscription(topicName = "ping-topic", lockDuration = 10000)
@Slf4j
public class PingTask extends AbstractExternalTask {

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
        try {
            // Simulate some processing logic
            log.info("Processing ping task with ID: {}", externalTask.getId());
            // Complete the task successfully
            externalTaskService.complete(externalTask);
        } catch (Exception e) {
            // Handle any exceptions and report failure to Camunda
            exceptionHandler(externalTaskService, externalTask, e);
        }
    }
}
