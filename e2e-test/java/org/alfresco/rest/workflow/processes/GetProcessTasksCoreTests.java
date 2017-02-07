package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.GroupModel;
import org.alfresco.utility.model.ProcessModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetProcessTasksCoreTests extends RestTest
{
    private FileModel document, document1, document2;
    private SiteModel siteModel;
    private UserModel adminUser, userWhoStartsProcess, candidate, anotherAssignee, assignee;
    private ProcessModel process;
    private GroupModel group;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsProcess = dataUser.createRandomTestUser();
        candidate = dataUser.createRandomTestUser();
        anotherAssignee = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsProcess).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document1 = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document2 = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        process = dataWorkflow.usingUser(userWhoStartsProcess).usingSite(siteModel).usingResource(document)
                .createSingleReviewerTaskAndAssignTo(assignee);
        group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, candidate);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "getProcessTasks with user that is candidate with REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessTasksWithUserThatIsCandidate() throws Exception
    {
        ProcessModel processModel = dataWorkflow.usingUser(userWhoStartsProcess).usingSite(siteModel).usingResource(document1)
                .createGroupReviewTaskAndAssignTo(group);

        restClient.authenticateUser(candidate).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsNotEmpty()
                .and().entriesListContains("assignee", candidate.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify getProcessTasks using admin from same network with REST API and status code is OK")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void getProcessTasksWithAdminFromSameNetwork() throws Exception
    {
        UserModel adminTenant = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant);
        SiteModel siteModel = dataSite.usingUser(adminTenant).createPublicRandomSite();
        UserModel tenantAssignee = dataUser.usingUser(adminTenant).createUserWithTenant("uTenant");
        dataContent.usingUser(adminTenant).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);

        RestProcessModel processModel = restClient.authenticateUser(tenantAssignee).withWorkflowAPI().addProcess("activitiAdhoc", tenantAssignee, false, CMISUtil.Priority.Normal);

        restClient.authenticateUser(adminTenant).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify getProcessTasks using admin from different network with REST API and status code is FORBIDDEN (403)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void getProcessTasksWithAdminFromDifferentNetwork() throws Exception
    {
        UserModel adminTenant, secondAdminTenant, tenantAssignee;
        adminTenant = UserModel.getAdminTenantUser();
        secondAdminTenant = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant);

        SiteModel siteModel = dataSite.usingUser(adminTenant).createPublicRandomSite();
        dataContent.usingUser(adminTenant).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        tenantAssignee = dataUser.usingUser(adminTenant).createUserWithTenant("uTenant");
        RestProcessModel processModel = restClient.authenticateUser(adminTenant).withWorkflowAPI().addProcess("activitiAdhoc", tenantAssignee, false, CMISUtil.Priority.Normal);

        restClient.authenticateUser(adminUser).usingTenant().createTenant(secondAdminTenant);
        restClient.authenticateUser(secondAdminTenant).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "getProcessTasks for invalid processId with REST API and status code is NOT_FOUND (404)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessTasksUsingInvalidProcessId() throws Exception
    {
        ProcessModel processModel = process;
        processModel.setId("invalidProcessId");
        restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "getProcessTasks for empty processId with REST API and status code is NOT_FOUND (404)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessTasksUsingEmptyProcessId() throws Exception
    {
        ProcessModel processModel = process;
        processModel.setId("");
        restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "User completes task then getProcessTasks with REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void completeTaskThenGetProcessTasks() throws Exception
    {
        ProcessModel processModel = dataWorkflow.usingUser(userWhoStartsProcess).usingSite(siteModel).usingResource(document2)
                .createMoreReviewersWorkflowAndAssignTo(anotherAssignee);
        RestTaskModel restTaskModel = restClient.authenticateUser(anotherAssignee).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsNotEmpty().getOneRandomEntry().onModel();
        restTaskModel = restClient.withParams("select=state").withWorkflowAPI().usingTask(restTaskModel).updateTask("completed");
        restTaskModel.assertThat().field("id").is(restTaskModel.getId()).and().field("state").is("completed");
        restClient.withWorkflowAPI().getTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
