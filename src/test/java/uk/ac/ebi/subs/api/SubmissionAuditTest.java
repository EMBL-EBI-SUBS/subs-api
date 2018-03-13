package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.tsc.aap.client.security.WithMockAAPUser;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@ActiveProfiles("aap")
public class SubmissionAuditTest {

    public static final String DEFAULT_USER_REFERENCE = "usr-12345";
    public static final String USI_USER = "usi_user";
    public static final String USI_USER_EMAIL = "usi-user@usi.org";
    public static final String USER_FULL_NAME = "Test User";
    private MockMvc mockMvc;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    protected ObjectMapper objectMapper;

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .build();
    }

    @After
    public void finish() {
        submissionRepository.deleteAll();
    }

    @Test
    @WithMockAAPUser(userName = USI_USER,email = USI_USER_EMAIL,userReference = DEFAULT_USER_REFERENCE, fullName = USER_FULL_NAME, domains = {Helpers.TEAM_NAME})
    public void postSubmissionAndCheckAAPAuditInfo () throws Exception {
        final Submission submission = postSubmission();
        Assert.assertEquals(submission.getCreatedBy(),DEFAULT_USER_REFERENCE);
    }

    @Test
    @WithMockUser(username= USI_USER,roles={Helpers.TEAM_NAME})
    public void postSubmissionAndCheckAuditInfo () throws Exception {
        final Submission submission = postSubmission();
        Assert.assertEquals(submission.getCreatedBy(),USI_USER);
    }

    private Submission postSubmission() throws Exception {
        uk.ac.ebi.subs.data.Submission clientSubmission = new uk.ac.ebi.subs.data.Submission();
        clientSubmission.setSubmitter(new Submitter());
        clientSubmission.getSubmitter().setEmail(USI_USER_EMAIL);
        Team team = new Team();
        team.setName(Helpers.TEAM_NAME);
        clientSubmission.setTeam(team);
        String submissionJson = objectMapper.writeValueAsString(clientSubmission);
        this.mockMvc.perform(
                post("/submissions").content(submissionJson)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(RestMediaTypes.HAL_JSON)).andExpect(status().isCreated());
        return submissionRepository.findAll().get(0);
    }

}
