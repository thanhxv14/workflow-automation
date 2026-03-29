package com.thanhxv.core.config;

import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.spring.boot.starter.configuration.impl.AbstractCamundaConfiguration;
import org.springframework.stereotype.Component;

@Component
public class CamundaGroupConfig extends AbstractCamundaConfiguration {

    @Override
    public void postProcessEngineBuild(ProcessEngine processEngine) {
        IdentityService identityService = processEngine.getIdentityService();

        // Create group if not exists
        if (identityService.createGroupQuery().groupId("creditofficers").singleResult() == null) {
            Group group = identityService.newGroup("creditofficers");
            group.setName("Credit Officers");
            group.setType("WORKFLOW");
            identityService.saveGroup(group);
        }

        // Create demo officer user if not exists
        if (identityService.createUserQuery().userId("officer1").singleResult() == null) {
            User officer = identityService.newUser("officer1");
            officer.setFirstName("Doan");
            officer.setLastName("Tristan");
            officer.setEmail("officer1@bank.vn");
            officer.setPassword("officer123");
            identityService.saveUser(officer);
            identityService.createMembership("officer1", "creditofficers");
        }
    }
}
