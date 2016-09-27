package org.alfresco.rest.demo;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.requests.RestCommentsApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "comments", "sanity" })
public class SampleCommentsTest extends RestTest
{
    @Autowired
    RestCommentsApi commentsAPI;

    private UserModel userModel;
    private FolderModel folderModel;
    private SiteModel siteModel;
    private FileModel document;

    @BeforeClass
    public void initTest() throws Exception
    {
        userModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        restClient.authenticateUser(userModel);
        commentsAPI.useRestClient(restClient);

        document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify admin user adds comments with Rest API and status code is 200")
    public void admiShouldAddComment() throws JsonToModelConversionException, Exception
    {
        commentsAPI.addComment(document.getNodeRef(), "This is a new comment");
        commentsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.CREATED.toString());
    }

    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify admin user gets comments with Rest API and status code is 200")
    public void admiShouldRetrieveComments() throws JsonToModelConversionException
    {
        commentsAPI.getNodeComments(document.getNodeRef());
        commentsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify admin user updates comments with Rest API")
    public void adminShouldUpdateComment() throws JsonToModelConversionException, Exception
    {
        // add initial comment
        String commentId = commentsAPI.addComment(document.getNodeRef(), "This is a new comment").getId();

        // update comment
        RestCommentModel commentEntry = commentsAPI.updateComment(document.getNodeRef(), commentId, "This is the updated comment");
        commentEntry.assertCommentContentIs("This is the updated comment");
    }

}