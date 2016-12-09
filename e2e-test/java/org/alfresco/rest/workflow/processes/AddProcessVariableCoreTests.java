package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessVariableModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddProcessVariableCoreTests extends RestTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsProcess, assignee, adminUser, anotherUser, adminTenantUser, adminTenantUser2, tenantUserAssignee;
    private RestProcessModel processModel;
    private RestProcessVariableModel variableModel, processVariable;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        anotherUser = dataUser.createRandomTestUser();
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsProcess).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsProcess).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Add process variable using by the user who started the process.")
    public void addProcessVariableByUserThatStartedTheProcess() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, Priority.Normal);

        processVariable = restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processVariable.assertThat().field("name").is(processVariable.getName()).and().field("type").is(processVariable.getType()).and().field("value")
                .is(processVariable.getValue());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Add process variable using by a random user.")
    public void addProcessVariableByAnyUser() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(anotherUser).withWorkflowAPI().addProcess("activitiAdhoc", anotherUser, false, Priority.Normal);

        processVariable = restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processVariable.assertThat().field("name").is(processVariable.getName()).and().field("type").is(processVariable.getType()).and().field("value")
                .is(processVariable.getValue());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Add process variable using by admin in other network.")
    public void addProcessVariableByAdminInOtherNetwork() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");

        adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser2);

        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false, Priority.Normal);

        restClient.authenticateUser(adminTenantUser2);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processVariable = restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process variable is falling in case invalid type is provided")
    public void failedAddingProcessVariableIfInvalidTypeIsProvided() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:textarea");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Unsupported type of variable: 'd:textarea'.");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @Bug(id = "ACE-5674")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process variable is falling in case invalid type prefix is provided")
    public void failedAddingProcessVariableIfInvalidTypePrefixIsProvided() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("ddt:text");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Namespace prefix ddt is not mapped to a namespace URI");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process variable is falling in case invalid value is provided")
    public void failedAddingProcessVariableIfInvalidValueIsProvided() throws Exception
    {
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:int");
        variableModel.setValue("invalidValue");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("For input string: \"invalidValue\"");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process variable is falling in case missing required variable body (name) is provided")
    public void failedAddingProcessVariableIfMissingRequiredVariableBodyIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();

        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"value\": \"missingVariableName\",\"type\": \"d:text\"}",
                "processes/{processId}/variables/{variableName}", processModel.getId(), variableModel.getName());
        restClient.processModel(RestProcessVariableModel.class, request);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Variable name is required.");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process variable is falling in case invalid variableBody (adding extra parameter in body) is provided")
    public void failedAddingProcessVariableIfInvalidBodyIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();

        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"value\": \"testing\",\"type\": \"d:text\"}",
                "processes/{processId}/variables/{variableName}", processModel.getId(), variableModel.getName());
        restClient.processModel(RestProcessVariableModel.class, request);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary("Could not read content from HTTP request body: Unrecognized field \"scope\"");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process variable is falling in case empty name is provided")
    public void failedAddingProcessVariableIfEmptyNameIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        variableModel.setName("");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED).assertLastError().containsSummary(RestErrorModel.PUT_EMPTY_ARGUMENT);
    }

}
