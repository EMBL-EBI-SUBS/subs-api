package uk.ac.ebi.subs.api.resourceAssembly;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.DomainController;
import uk.ac.ebi.subs.api.SubmissionContentsController;
import uk.ac.ebi.subs.data.Submission;
import uk.ac.ebi.subs.data.SubmissionLinks;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;


@Component
public class SubmissionResourceProcessor implements ResourceProcessor<Resource<Submission>> {

    private Class<SubmissionContentsController> submittablesControllerClass = SubmissionContentsController.class;

    private Pageable defaultPageRequest() {
        return new PageRequest(0, 1);
    }


    @Override
    public Resource<Submission> process(Resource<Submission> resource) {

        if (resource.getContent().getDomain() != null && resource.getContent().getDomain().getName() != null) {
            resource.add(
                    linkTo(
                            methodOn(DomainController.class)
                                    .getDomain(resource.getContent().getDomain().getName())
                    ).withRel("domain")
            );
        }

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionAnalyses(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.ANALYSIS)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionAssays(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.ASSAY)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionAssayData(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.ASSAY_DATA)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionEgaDacs(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.EGA_DAC)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionEgaDacPolicies(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.EGA_DAC_POLICY)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionEgaDatasets(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.EGA_DATASET)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionProjects(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.PROJECT)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionProtocols(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.PROTOCOL)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionSamples(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.SAMPLE)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionSampleGroups(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.SAMPLE_GROUP)
        );

        resource.add(
                linkTo(
                        methodOn(submittablesControllerClass)
                                .submissionStudies(
                                        resource.getContent().getId(),
                                        defaultPageRequest()
                                ))
                        .withRel(SubmissionLinks.STUDY)
        );

        return resource;
    }


}
