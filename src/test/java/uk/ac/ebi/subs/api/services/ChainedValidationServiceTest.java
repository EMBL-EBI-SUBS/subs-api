package uk.ac.ebi.subs.api.services;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.api.Helpers;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.StoredSubmittable;
import uk.ac.ebi.subs.repository.model.Study;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;
import uk.ac.ebi.subs.repository.repos.submittables.StudyRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static uk.ac.ebi.subs.api.Helpers.generateTestTeam;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@WithMockUser(username = "usi_user", roles = {Helpers.TEAM_NAME})
@ActiveProfiles("SubmittableValidationDispatcherTest")
public class ChainedValidationServiceTest {

    @Autowired
    private ChainedValidationService service;

    @Autowired
    private SubmissionRepository submissionRepository;
    @Autowired
    private SampleRepository sampleRepository;
    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private SubmittableValidationDispatcher submittableValidationDispatcher;

    @MockBean
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    private Submission submission;
    private Study study;

    @Before
    public void setUp() {
        clearDB();
        submittableValidationDispatcher.setRabbitMessagingTemplate(rabbitMessagingTemplate);

        submission = createSubmission();
        submissionRepository.insert(submission);
        createSamples();
        study = createStudy();
    }

    @Test
    public void findSubmittablesInSubmissionTest() {
        Map<String, List<? extends StoredSubmittable>> submittablesInSubmission = service.findSubmittablesInSubmission(submission.getId());
        Assert.assertEquals(3, submittablesInSubmission.get("Sample").size());
        Assert.assertEquals(0, submittablesInSubmission.get("Assay").size());
        Assert.assertEquals(1, submittablesInSubmission.get("Study").size());
    }

    @Test
    public void filterOutTriggerSubmittableTest() {
        Map<String, List<? extends StoredSubmittable>> submittablesInSubmission = service.findSubmittablesInSubmission(submission.getId());

        service.filterOutTriggerSubmittable(study, submittablesInSubmission);
        Assert.assertEquals(0, submittablesInSubmission.get("Study").size());

        Assert.assertEquals(3, submittablesInSubmission.get("Sample").size());
        Assert.assertEquals(0, submittablesInSubmission.get("Assay").size());
    }

    @Test
    public void triggerChainedValidationTest() {
        service.triggerChainedValidation(study);
        verify(submittableValidationDispatcher, atLeast(3)).validateUpdate(any(Sample.class));
    }

    private Submission createSubmission() {
        Submission sub = new Submission();
        sub.setId(UUID.randomUUID().toString());
        Team d = generateTestTeam();
        sub.setTeam(d);
        return sub;
    }

    private void createSamples() {
        List<Sample> samples = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Sample sample = new Sample();
            sample.setId(UUID.randomUUID().toString());
            samples.add(sample);
        }

        for (Sample sample : samples) {
            sample.setSubmission(submission);
        }
        sampleRepository.insert(samples);
    }

    private Study createStudy() {
        Study study = new Study();
        study.setId(UUID.randomUUID().toString());
        study.setSubmission(submission);
        Team d = generateTestTeam();
        study.setTeam(d);
        return studyRepository.insert(study);
    }

    private void clearDB() {
        submissionRepository.deleteAll();
        sampleRepository.deleteAll();
        studyRepository.deleteAll();
    }
}
