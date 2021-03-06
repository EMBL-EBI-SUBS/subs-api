package uk.ac.ebi.subs.api.controllers;

import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.hateoas.Resource;
import org.springframework.security.access.method.P;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.subs.api.processors.LinkHelper;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.subs.repository.security.PreAuthorizeParamTeamName;

/**
 * Retrieve the list of items by given team and not filtered by submission.
 *
 * The list contains only 1 sample if the same sample has been updated/submitted for many times.
 */
@RestController
@BasePathAwareController
public class TeamItemsController {

    private LinkHelper linkHelper;

    public TeamItemsController(LinkHelper linkHelper) {
        this.linkHelper = linkHelper;
    }

    @PreAuthorizeParamTeamName
    @RequestMapping("/teams/{teamName:.+}/items")
    public Resource<TeamItems> teamItems(@PathVariable @P("teamName") String teamName) {

        Team team = Team.build(teamName);

        return this.process(new Resource<>(new TeamItems(team)));
    }

    public Resource<TeamItems> process(Resource<TeamItems> resource) {
        linkHelper.addSubmittablesInTeamLinks(resource.getLinks(), resource.getContent().getTeam().getName());

        resource.getContent().setTeam(null);

        return resource;
    }

    public class TeamItems {
        private Team team;

        public Team getTeam() {
            return team;
        }

        public void setTeam(Team team) {
            this.team = team;
        }

        public TeamItems(Team team) {
            this.team = team;
        }
    }
}
