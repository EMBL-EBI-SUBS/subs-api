package uk.ac.ebi.subs.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.webmvc.RestMediaTypes;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.hypermedia.LinkDescriptor;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.operation.preprocess.ContentModifier;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.subs.ApiApplication;
import uk.ac.ebi.subs.DocumentationProducer;
import uk.ac.ebi.subs.api.handlers.SubmissionEventHandler;
import uk.ac.ebi.subs.api.handlers.SubmissionStatusEventHandler;
import uk.ac.ebi.subs.api.services.SubmissionEventService;
import uk.ac.ebi.subs.data.component.Archive;
import uk.ac.ebi.subs.data.component.Domain;
import uk.ac.ebi.subs.data.component.SampleRelationship;
import uk.ac.ebi.subs.data.component.Submitter;
import uk.ac.ebi.subs.repository.model.Sample;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;
import uk.ac.ebi.subs.repository.repos.SubmissionRepository;
import uk.ac.ebi.subs.repository.repos.status.ProcessingStatusRepository;
import uk.ac.ebi.subs.repository.repos.status.SubmissionStatusRepository;
import uk.ac.ebi.subs.repository.repos.submittables.SampleRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Use this class to create document snippets. Ascii docotor will weave them into html documents,
 * using the files in src/resources/docs/ascidocs
 *
 * @see <a href="https://github.com/EBISPOT/OLS/blob/master/ols-web/src/test/java/uk/ac/ebi/spot/ols/api/ApiDocumentation.java">OLS ApiDocumentation.java</a>
 * <p>
 * gives this
 * @see <a href="http://www.ebi.ac.uk/ols/docs/api">OLS API Docs<</a>
 * <p>
 * API documentation should learn from the excellent example at @see <a href="https://developer.github.com/v3/">GitHub</a>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = ApiApplication.class)
@Category(DocumentationProducer.class)
public class ApiDocumentation {

    @Rule
    public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("build/generated-snippets");

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SubmissionStatusRepository submissionStatusRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private ProcessingStatusRepository processingStatusRepository;

    @Autowired
    private SubmissionEventHandler submissionEventHandler;

    @Autowired
    private SubmissionStatusEventHandler submissionStatusEventHandler;

    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private SubmissionEventService fakeSubmissionEventService = new SubmissionEventService() {
        @Override
        public void submissionCreated(Submission submission) {

        }

        @Override
        public void submissionUpdated(Submission submission) {

        }

        @Override
        public void submissionDeleted(Submission submission) {

        }

        @Override
        public void submissionSubmitted(Submission submission) {

        }
    };

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        submissionEventHandler.setSubmissionEventService(fakeSubmissionEventService);
        submissionStatusEventHandler.setSubmissionEventService(fakeSubmissionEventService);

        clearDatabases();

        MockMvcRestDocumentationConfigurer docConfig = documentationConfiguration(this.restDocumentation);

        docConfig.uris()
                .withScheme("http")
                .withHost("www.ebi.ac.uk/submissions")
                .withPort(80);

        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(docConfig)
                .build();
    }

    private void clearDatabases() {
        this.submissionRepository.deleteAll();
        this.sampleRepository.deleteAll();
        this.submissionStatusRepository.deleteAll();
    }

    @After
    public void tearDown() {
        clearDatabases();
    }

    @Test
    public void invalidJson() throws Exception {


        this.mockMvc.perform(
                post("/api/submissions").content("Tyger Tyger, burning bright, In the forests of the night")
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("invalid-json",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(),
                                responseFields(
                                        fieldWithPath("cause").description("Cause of the error"),
                                        fieldWithPath("message").description("Error message")

                                )
                        )
                );

    }

    @Test
    public void jsonArrayInsteadOfObject() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = goodClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(Arrays.asList(submission, submission));


        this.mockMvc.perform(
                post("/api/submissions").content(jsonRepresentation)
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("json-array-instead-of-object",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(),
                                responseFields(
                                        fieldWithPath("cause").description("Cause of the error"),
                                        fieldWithPath("message").description("Error message")

                                )
                        )
                );

    }

    private uk.ac.ebi.subs.data.Submission goodClientSubmission() {
        uk.ac.ebi.subs.data.Submission submission = new uk.ac.ebi.subs.data.Submission();
        submission.setDomain(new Domain());
        submission.getDomain().setName("my-team");
        submission.setSubmitter(new Submitter());
        submission.getSubmitter().setEmail("alice@test.org");
        return submission;
    }

    @Test
    public void invalidSubmission() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = badClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(submission);


        this.mockMvc.perform(
                post("/api/submissions").content(jsonRepresentation)
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isBadRequest())
                .andDo(
                        document("invalid-submission",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("errors").description("List of errors"),
                                        fieldWithPath("errors[0].entity").description("Type of the entity with the error"),
                                        fieldWithPath("errors[0].property").description("Path of the field with the error"),
                                        fieldWithPath("errors[0].invalidValue").description("Value of the field that has caused the error"),
                                        fieldWithPath("errors[0].message").description("Message describing the error")

                                )
                        )
                );

    }

    @Test
    public void validSubmission() throws Exception {
        uk.ac.ebi.subs.data.Submission submission = goodClientSubmission();

        String jsonRepresentation = objectMapper.writeValueAsString(submission);


        this.mockMvc.perform(
                post("/api/submissions").content(jsonRepresentation)
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isCreated())
                .andDo(
                        document("create-submission",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("submitter").description("Submitter who is responsible for this submission"),
                                        fieldWithPath("domain").description("Domain this submission belongs to"),

                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("submission").description("This submission"),
                                        linkWithRel("self:update").description("This submission can be updated"),
                                        linkWithRel("self:delete").description("This submission can be deleted"),
                                        linkWithRel("domain").description("The domain this submission belongs to"),
                                        linkWithRel("analyses").description("Analyses within this submission"),
                                        linkWithRel("assays").description("Assays within this submission"),
                                        linkWithRel("assayData").description("Assay data within this submission"),
                                        linkWithRel("egaDacs").description("DACs within this submission"),
                                        linkWithRel("egaDacPolicies").description("DAC policies within this submission"),
                                        linkWithRel("egaDatasets").description("EGA datasets within this submission"),
                                        linkWithRel("projects").description("Projects within this submission"),
                                        linkWithRel("protocols").description("Protocols within this submission"),
                                        linkWithRel("samples").description("Samples within this submission"),
                                        linkWithRel("sampleGroups").description("Sample groups within this submission"),
                                        linkWithRel("studies").description("Studies within this submission"),
                                        linkWithRel("submissionStatus").description("Status of this submission"),
                                        linkWithRel("sampleGroups:create").description("This submission can accept new sample groups"),
                                        linkWithRel("analyses:create").description("This submission can accept new analyses"),
                                        linkWithRel("egaDatasets:create").description("This submission can accept new EGA datasets"),
                                        linkWithRel("projects:create").description("This submission can accept new projects"),
                                        linkWithRel("assays:create").description("This submission can accept new assays"),
                                        linkWithRel("protocols:create").description("This submission can accept new protocols"),
                                        linkWithRel("assayData:create").description("This submission can accept new assay data"),
                                        linkWithRel("egaDacs:create").description("This submission can accept new DACs"),
                                        linkWithRel("samples:create").description("This submission can accept new samples"),
                                        linkWithRel("egaDacPolicies:create").description("This submission can accept new DAC policies"),
                                        linkWithRel("studies:create").description("This submission can accept new studies")
                                )
                        )
                );

        SubmissionStatus status = submissionStatusRepository.findAll().get(0);
        Assert.notNull(status);

        this.mockMvc.perform(
                patch("/api/submissionStatuses/{id}",status.getId()).content("{\"status\": \"Submitted\"}")
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("patch-submission-status",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("status").description("Current status value"),

                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("submissionStatus").description("This resource"),
                                        linkWithRel("statusDescription").description("Description of this status")
                                )
                        )
                );

    }

    @Test
    public void createSample() throws Exception {
        Submission sub = storeSubmission();
        uk.ac.ebi.subs.data.client.Sample sample = Helpers.generateTestClientSamples(1).get(0);

        sample.setSubmission("http://www.ebi.ac.uk/api/submissions/"+sub.getId());

        String jsonRepresentation = objectMapper.writeValueAsString(sample);


        this.mockMvc.perform(
                post("/api/samples").content(jsonRepresentation)
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isCreated())
                .andDo(
                        document("create-sample",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the sample within the domain"),
                                        fieldWithPath("title").description("Title for the sample"),
                                        fieldWithPath("description").description("Description for the sample"),
                                        fieldWithPath("attributes").description("A list of attributes for the sample"),
                                        fieldWithPath("sampleRelationships").description("Relationships to other samples"),
                                        fieldWithPath("taxonId").description("NCBI Taxon ID for this sample"),
                                        fieldWithPath("taxon").description("Scientific name for this taxon"),
                                        fieldWithPath("_embedded.submission").description("Submission that this sample is part of"),
                                        fieldWithPath("domain").description("Domain this sample belongs to"),

                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("sample").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("submission").description("Submission that this sample is part of"),
                                        linkWithRel("processingStatus").description("Processing status for this sample"),
                                        linkWithRel("history").description("Collection of resources for samples with the same domain and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by domain and alias")

                                )
                        )
                );


        String sampleId = sampleRepository.findAll().get(0).getId();
        SampleRelationship sampleRelationship = new SampleRelationship();
        sampleRelationship.setAlias("D0");
        sampleRelationship.setDomain(sub.getDomain().getName());
        sampleRelationship.setRelationshipNature("Child of");

        sample.getSampleRelationships().add(sampleRelationship);

        jsonRepresentation = objectMapper.writeValueAsString(sample);

        this.mockMvc.perform(
                put("/api/samples/{id}",sampleId).content(jsonRepresentation)
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("update-sample",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the sample within the domain"),
                                        fieldWithPath("title").description("Title for the sample"),
                                        fieldWithPath("description").description("Description for the sample"),
                                        fieldWithPath("attributes").description("A list of attributes for the sample"),
                                        fieldWithPath("sampleRelationships").description("Relationships to other samples"),
                                        fieldWithPath("taxonId").description("NCBI Taxon ID for this sample"),
                                        fieldWithPath("taxon").description("Scientific name for this taxon"),
                                        fieldWithPath("_embedded.submission").description("Submission that this sample is part of"),
                                        fieldWithPath("domain").description("Domain this sample belongs to"),

                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("sample").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("submission").description("Submission that this sample is part of"),
                                        linkWithRel("processingStatus").description("Processing status for this sample"),
                                        linkWithRel("history").description("Collection of resources for samples with the same domain and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by domain and alias")

                                )
                        )
                );

        this.mockMvc.perform(
                patch("/api/samples/{id}",sampleId).content("{\"archive\":\"BioSamples\"}")
                        .contentType(RestMediaTypes.HAL_JSON)
                        .accept(RestMediaTypes.HAL_JSON)

        ).andExpect(status().isOk())
                .andDo(
                        document("patch-sample",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("_links").description("Links"),
                                        fieldWithPath("alias").description("Unique name for the sample within the domain"),
                                        fieldWithPath("title").description("Title for the sample"),
                                        fieldWithPath("description").description("Description for the sample"),
                                        fieldWithPath("attributes").description("A list of attributes for the sample"),
                                        fieldWithPath("sampleRelationships").description("Relationships to other samples"),
                                        fieldWithPath("taxonId").description("NCBI Taxon ID for this sample"),
                                        fieldWithPath("taxon").description("Scientific name for this taxon"),
                                        fieldWithPath("_embedded.submission").description("Submission that this sample is part of"),
                                        fieldWithPath("domain").description("Domain this sample belongs to"),
                                        fieldWithPath("archive").description("Domain this sample belongs to"),
                                        fieldWithPath("createdDate").description("Date this resource was created"),
                                        fieldWithPath("lastModifiedDate").description("Date this resource was modified"),
                                        fieldWithPath("createdBy").description("User who created this resource"),
                                        fieldWithPath("lastModifiedBy").description("User who last modified this resource")
                                ),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("sample").description("This resource"),
                                        linkWithRel("self:update").description("This resource can be updated"),
                                        linkWithRel("self:delete").description("This resource can be deleted"),
                                        linkWithRel("submission").description("Submission that this sample is part of"),
                                        linkWithRel("processingStatus").description("Processing status for this sample"),
                                        linkWithRel("history").description("Collection of resources for samples with the same domain and alias as this resource"),
                                        linkWithRel("current-version").description("Current version of this sample, as identified by domain and alias")

                                )
                        )
                );

    }



    private uk.ac.ebi.subs.data.Submission badClientSubmission() {
        return new uk.ac.ebi.subs.data.Submission();
    }

    private ContentModifyingOperationPreprocessor maskEmbedded() {
        return new ContentModifyingOperationPreprocessor(new MaskElement("_embedded"));
    }

    private ContentModifyingOperationPreprocessor maskLinks() {
        return new ContentModifyingOperationPreprocessor(new MaskElement("_links"));
    }

    @Test
    public void pageExample() throws Exception {

        String domainName = null;
        for (int i = 0; i < 50; i++) {
            Submission s = Helpers.generateTestSubmission();
            submissionStatusRepository.insert(s.getSubmissionStatus());
            submissionRepository.insert(s);
            domainName = s.getDomain().getName();
        }

        this.mockMvc.perform(get("/api/submissions/search/by-domain?domainName={domainName}&page=1&size=10", domainName))
                .andExpect(status().isOk())
                .andDo(document(
                        "page-example",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(maskEmbedded(), prettyPrint()),

                        links(halLinks(),
                                linkWithRel("self").description("This resource list"),
                                linkWithRel("first").description("The first page in the resource list"),
                                linkWithRel("next").description("The next page in the resource list"),
                                linkWithRel("prev").description("The previous page in the resource list"),
                                linkWithRel("last").description("The last page in the resource list")
                        ),
                        responseFields(
                                fieldWithPath("_links").description("<<resources-page-links,Links>> to other resources"),
                                fieldWithPath("_embedded").description("The list of resources"),
                                fieldWithPath("page.size").description("The number of resources in this page"),
                                fieldWithPath("page.totalElements").description("The total number of resources"),
                                fieldWithPath("page.totalPages").description("The total number of pages"),
                                fieldWithPath("page.number").description("The page number")
                        )
                ));
    }

    @Test
    public void conditionalRequests() throws Exception {
        Submission sub = storeSubmission();
        List<Sample> samples = storeSamples(sub, 1);
        Sample s = samples.get(0);

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM YYYY H:m:s zzz");

        String etagValueString = String.format("ETag: \"%d\"", s.getVersion());
        String lastModifiedString = dateFormat.format(s.getLastModifiedDate());

        this.mockMvc.perform(
                get("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-None-Match", etagValueString)
        ).andExpect(status().isNotModified())
                .andDo(
                        document("conditional-fetch-etag-get-if-none-match",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );

        this.mockMvc.perform(
                delete("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-Match", "ETag: \"10\"")
        ).andExpect(status().isPreconditionFailed())
                .andDo(
                        document("conditional-delete-if-etag-match",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );

        this.mockMvc.perform(
                get("/api/samples/{sampleId}", s.getId())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("If-Modified-Since", lastModifiedString)
        ).andExpect(status().isNotModified())
                .andDo(
                        document("conditional-fetch-if-modified-since",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()
                                )
                        )
                );


    }


    @Test
    public void sampleList() throws Exception {
        Submission sub = storeSubmission();
        List<Sample> samples = storeSamples(sub, 30);

        this.mockMvc.perform(
                get("/api/samples/search/by-submission?submissionId={submissionId}&size=2", sub.getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("samples/by-submission",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        nextRelLink(),
                                        firstRelLink(),
                                        lastRelLink()
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("_embedded.samples").description("Samples within the submission"),
                                        paginationPageSizeDescriptor(),
                                        paginationTotalElementsDescriptor(),
                                        paginationTotalPagesDescriptor(),
                                        paginationPageNumberDescriptor()
                                )
                        )
                );

        this.mockMvc.perform(
                get("/api/samples/{sample}", samples.get(0).getId())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("samples/fetch-one",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink(),
                                        processingStatusLink(),
                                        submissionLink(),
                                        linkWithRel("sample").description("Link to this sample"),
                                        linkWithRel("self:update").description("This sample can be updated"),
                                        linkWithRel("self:delete").description("This sample can be deleted")
                                ),
                                responseFields( //TODO fill out the descriptions
                                        linksResponseField(),
                                        fieldWithPath("alias").description(""),
                                        fieldWithPath("title").description(""),
                                        fieldWithPath("description").description(""),
                                        fieldWithPath("sampleRelationships").description(""),
                                        fieldWithPath("taxonId").description(""),
                                        fieldWithPath("taxon").description(""),
                                        fieldWithPath("attributes").description(""),
                                        fieldWithPath("createdDate").description(""),
                                        fieldWithPath("lastModifiedDate").description(""),
                                        fieldWithPath("createdBy").description(""),
                                        fieldWithPath("lastModifiedBy").description(""),
                                        fieldWithPath("_embedded.submission").description("")


                                )
                        ));
    }

    private List<Sample> storeSamples(Submission sub, int numberRequired) {
        List<Sample> samples = Helpers.generateTestSamples(numberRequired);

        for (Sample s : samples) {
            s.setCreatedDate(new Date());
            s.setSubmission(sub);
            processingStatusRepository.insert(s.getProcessingStatus());
            sampleRepository.insert(s);
        }
        return samples;
    }

    @Test
    public void samplesSearchResource() throws Exception {
        this.mockMvc.perform(
                get("/api/samples/search")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("samples-search-resource",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("history").description("Search for all versions of a sample by domain and alias "),
                                        linkWithRel("by-submission").description("Search for all samples within a submission"),
                                        linkWithRel("by-domain").description("Search for samples within a domain"),
                                        linkWithRel("current-version").description("Find the current version of a sample by domain and alias")
                                ),
                                responseFields(
                                        linksResponseField()
                                )
                        )
                );
    }

    @Test
    public void domain() throws Exception {

        Submission s = storeSubmission();
        Domain d = s.getDomain();

        this.mockMvc.perform(
                get("/api/domains/{domainName}", d.getName())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("get-domain",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        linkWithRel("self").description("This resource"),
                                        linkWithRel("submissions").description("Collection of submissions within this domain"),
                                        linkWithRel("analyses").description("Collection of analyses within this domain"),
                                        linkWithRel("assays").description("Collection of assays within this domain"),
                                        linkWithRel("assayData").description("Collection of assay data within this domain"),
                                        linkWithRel("egaDacs").description("Collection of DACs within this domain"),
                                        linkWithRel("egaDacPolicies").description("Collection of DAC policies within this domain"),
                                        linkWithRel("egaDatasets").description("Collection of EGA Datasets within this domain"),
                                        linkWithRel("projects").description("Collection of projects within this domain"),
                                        linkWithRel("protocols").description("Collection of protocols within this domain"),
                                        linkWithRel("samples").description("Collection of samples within this domain"),
                                        linkWithRel("sampleGroups").description("Collection of sample groups within this domain"),
                                        linkWithRel("studies").description("Collection of studies within this domain")
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("name").description("Name of this domain")
                                )
                        )
                );
    }

    @Test
    public void rootEndpoint() throws Exception {

        this.mockMvc.perform(
                get("/api")
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("root-endpoint",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(

                                        halLinks(),
                                        //domain
                                        linkWithRel("domain").description("Domain "),
                                        //submissions
                                        linkWithRel("submissions:search").description("Search resource for submissions"),
                                        linkWithRel("submissions:create").description("Create a new submission resource"),
                                        //submittables
                                        linkWithRel("analyses:create").description("Create a new analysis resource"),
                                        linkWithRel("analyses:search").description("Search resource for analyses"),
                                        linkWithRel("assayData:create").description("Create a new assay data resource"),
                                        linkWithRel("assayData:search").description("Search resource for assay data"),
                                        linkWithRel("assays:create").description("Create a new assay resource"),
                                        linkWithRel("assays:search").description("Search resource for assays"),
                                        linkWithRel("egaDacPolicies:create").description("Create a new DAC policy resource"),
                                        linkWithRel("egaDacPolicies:search").description("Search resource for policies"),
                                        linkWithRel("egaDacs:create").description("Create a new DAC resource"),
                                        linkWithRel("egaDacs:search").description("Search resource for DACs"),
                                        linkWithRel("egaDatasets:create").description("Create a new EGA dataset resource"),
                                        linkWithRel("egaDatasets:search").description("Search resource for EGA datasets"),
                                        linkWithRel("projects:create").description("Create a new project resource"),
                                        linkWithRel("projects:search").description("Search resource for projects"),
                                        linkWithRel("protocols:create").description("Create a new protocol resource"),
                                        linkWithRel("protocols:search").description("Search resource for protocols"),
                                        linkWithRel("sampleGroups:create").description("Create a new sample group resource"),
                                        linkWithRel("sampleGroups:search").description("Search resource for sample groups"),
                                        linkWithRel("samples:create").description("Create a new sample resource"),
                                        linkWithRel("samples:search").description("Search resource for samples"),
                                        linkWithRel("studies:create").description("Create a new study resource"),
                                        linkWithRel("studies:search").description("Search resource for studies"),
                                        //status descriptions
                                        linkWithRel("submissionStatusDescriptions").description("Collection resource for submission status descriptions"),
                                        linkWithRel("processingStatusDescriptions").description("Collection resource for processing status descriptions "),
                                        linkWithRel("releaseStatusDescriptions").description("Collection resource for release status descriptions"),
                                        //statuses
                                        linkWithRel("processingStatuses:search").description("Search resource for processing statuses"),
                                        //profile
                                        linkWithRel("profile").description("Application level details")
                                ),
                                responseFields(
                                        linksResponseField()
                                )
                        )
                );
    }

    @Test
    public void submissionsByDomain() throws Exception {

        Submission sub = storeSubmission();


        this.mockMvc.perform(
                get("/api/submissions/search/by-domain?domainName={domainName}", sub.getDomain().getName())
                        .accept(RestMediaTypes.HAL_JSON)
        ).andExpect(status().isOk())
                .andDo(
                        document("submissions/by-domain",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                links(
                                        halLinks(),
                                        selfRelLink()
                                ),
                                responseFields(
                                        linksResponseField(),
                                        fieldWithPath("_embedded.submissions").description("Submissions matching the domain name"),
                                        paginationPageSizeDescriptor(),
                                        paginationTotalElementsDescriptor(),
                                        paginationTotalPagesDescriptor(),
                                        paginationPageNumberDescriptor()
                                )
                        )
                );
    }

    private FieldDescriptor linksResponseField() {
        return fieldWithPath("_links").description("Links to other resources");
    }

    private LinkDescriptor selfRelLink() {
        return linkWithRel("self").description("Canonical link for this resource");
    }

    private Submission storeSubmission() {
        Submission sub = Helpers.generateTestSubmission();

        this.submissionStatusRepository.save(sub.getSubmissionStatus());
        this.submissionRepository.save(sub);
        return sub;
    }

    private FieldDescriptor paginationPageNumberDescriptor() {
        return fieldWithPath("page.number").description("The page number");
    }

    private FieldDescriptor paginationTotalPagesDescriptor() {
        return fieldWithPath("page.totalPages").description("The total number of pages");
    }

    private FieldDescriptor paginationTotalElementsDescriptor() {
        return fieldWithPath("page.totalElements").description("The total number of resources");
    }

    private FieldDescriptor paginationPageSizeDescriptor() {
        return fieldWithPath("page.size").description("The number of resources in this page");
    }

    private LinkDescriptor nextRelLink() {
        return linkWithRel("next").description("Next page of this resource");
    }

    private LinkDescriptor lastRelLink() {
        return linkWithRel("last").description("Last page for this resource");
    }

    private LinkDescriptor firstRelLink() {
        return linkWithRel("first").description("First page for this resource");
    }

    private LinkDescriptor prevRelLink() {
        return linkWithRel("prev").description("Previous page for this resource");
    }

    private LinkDescriptor submissionLink() {
        return linkWithRel("submission").description("Submission in which this record was created");
    }

    private LinkDescriptor processingStatusLink() {
        return linkWithRel("processingStatus").description("Current status of this record");
    }

    private class MaskElement implements ContentModifier {

        private String keyToRemove;

        public MaskElement(String keyToRemove) {
            this.keyToRemove = keyToRemove;
        }

        @Override
        public byte[] modifyContent(byte[] originalContent, MediaType contentType) {
            TypeReference<HashMap<String, Object>> typeRef
                    = new TypeReference<HashMap<String, Object>>() {
            };

            Map<String, Object> o = null;
            try {
                o = objectMapper.readValue(originalContent, typeRef);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            o.put("_embedded", "...");
            try {
                return objectMapper.writeValueAsBytes(o);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        }
    }

}
