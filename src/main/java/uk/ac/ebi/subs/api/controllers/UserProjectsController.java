package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.StoredSubmittableResourceProcessor;
import uk.ac.ebi.subs.api.services.IdentifiablePageToProjectionPage;
import uk.ac.ebi.subs.api.services.UserTeamService;
import uk.ac.ebi.subs.repository.model.Project;
import uk.ac.ebi.subs.repository.projections.SubmittableWithStatus;
import uk.ac.ebi.subs.repository.projections.UserProjectProjection;
import uk.ac.ebi.subs.repository.repos.submittables.ProjectRepository;

import java.util.List;

/**
 * Contains endpoints related the projects of the current user.
 */
@RestController
@BasePathAwareController
public class UserProjectsController {

    private UserTeamService userTeamService;
    private ProjectRepository projectRepository;
    private StoredSubmittableResourceProcessor<Project> storedSubmittableResourceProcessor;
    private IdentifiablePageToProjectionPage<Project, UserProjectProjection> identifiablePageToProjectionPage;

    public UserProjectsController(UserTeamService userTeamService, ProjectRepository projectRepository, StoredSubmittableResourceProcessor<Project> storedSubmittableResourceProcessor, IdentifiablePageToProjectionPage<Project, UserProjectProjection> identifiablePageToProjectionPage) {
        this.userTeamService = userTeamService;
        this.projectRepository = projectRepository;
        this.storedSubmittableResourceProcessor = storedSubmittableResourceProcessor;
        this.identifiablePageToProjectionPage = identifiablePageToProjectionPage;
    }

    /**
     * Retrieves a pageable list of the projects of the current user.
     * @param pageable pagination information
     * @return a pageable list of the projects of the current user.
     */
    @RequestMapping("/user/projects")
    public PagedResources<Resource<UserProjectProjection>> getUserProjects(Pageable pageable) {
        List<String> userTeamNames = userTeamService.userTeamNames();

        Page<Project> page = projectRepository.submittablesInTeams(userTeamNames, pageable);

        return identifiablePageToProjectionPage.convert(
                page,
                pageable,
                storedSubmittableResourceProcessor,
                UserProjectProjection.class
        );
    }
}
