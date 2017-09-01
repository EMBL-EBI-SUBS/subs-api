package uk.ac.ebi.subs.api.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.RepositoryLinksResource;
import org.springframework.data.rest.webmvc.support.RepositoryEntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.stereotype.Component;
import uk.ac.ebi.subs.api.controllers.StatusDescriptionController;
import uk.ac.ebi.subs.api.controllers.TeamController;
import uk.ac.ebi.subs.repository.model.ProcessingStatus;
import uk.ac.ebi.subs.repository.model.Submission;
import uk.ac.ebi.subs.repository.model.SubmissionStatus;

import java.util.Arrays;
import java.util.List;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@Component
public class RootEndpointLinkProcessor implements ResourceProcessor<RepositoryLinksResource> {

    private static final Logger logger = LoggerFactory.getLogger(RootEndpointLinkProcessor.class);

    public RootEndpointLinkProcessor(RepositoryEntityLinks repositoryEntityLinks, LinkHelper linkHelper) {
        this.repositoryEntityLinks = repositoryEntityLinks;
        this.linkHelper = linkHelper;
    }

    private RepositoryEntityLinks repositoryEntityLinks;
    private LinkHelper linkHelper;

    private void addLinks(List<Link> links) {
        addStatusDescriptions(links);
        addTeams(links);
    }

    @Override
    public RepositoryLinksResource process(RepositoryLinksResource resource) {
        logger.debug("processing resource: {}",resource.getLinks());
        clearAllLinks(resource);

        addLinks(resource.getLinks());

        return resource;
    }

    private void clearAllLinks(RepositoryLinksResource resource) {
        logger.debug("clearing links: {}",resource.getLinks());
        resource.removeLinks();
    }

      private void addTeams(List<Link> links) {
        links.add(
                linkTo(methodOn(TeamController.class).getTeams(null)
                ).withRel("teams")
        );
    }

    private void addStatusDescriptions(List<Link> links) {
        links.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allProcessingStatus(null))
                        .withRel("processingStatusDescriptions")
        );
        links.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allReleaseStatus(null))
                        .withRel("releaseStatusDescriptions")
        );
        links.add(
                linkTo(
                        methodOn(StatusDescriptionController.class)
                                .allSubmissionStatus(null))
                        .withRel("submissionStatusDescriptions")
        );
    }
}

