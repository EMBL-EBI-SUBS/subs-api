package uk.ac.ebi.subs.api.aap;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.data.component.Team;
import uk.ac.ebi.tsc.aap.client.model.Domain;
import uk.ac.ebi.tsc.aap.client.model.User;
import uk.ac.ebi.tsc.aap.client.repo.DomainService;
import uk.ac.ebi.tsc.aap.client.repo.ProfileService;
import uk.ac.ebi.tsc.aap.client.repo.TokenService;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TeamCreationService {

    @NonNull
    private TeamNameSequenceService teamNameSequenceService;

    @NonNull
    private UsiTokenService usiTokenService;

    @NonNull
    private DomainService domainService;

    @NonNull
    private ProfileService profileService;

    @NonNull
    private TokenService tokenService;

    public Team createTeam(User user, TeamDto teamDto) {

        String description = teamDto.getDescription();
        if (description == null) {
            description = "";
        }

        String teamName = teamNameSequenceService.nextTeamName();

        Domain domain = domainService.createDomain(teamName, description, usiTokenService.aapToken());
        domainService.addUserToDomain(domain, user, usiTokenService.aapToken());
        domainService.addManagerToDomain(domain, user, usiTokenService.aapToken());


        Map<String, String> domainProfileAttributes = new HashMap<>();
        domainProfileAttributes.put("centre name", teamDto.getCentreName());

        profileService.createDomainProfile(
                domain.getDomainReference(),
                domainProfileAttributes,
                usiTokenService.aapToken()
        );


        //remove USI as domain manager
        String usiUserRef = tokenService.getUserReference(usiTokenService.aapToken());
        User usiUser = User.builder().withReference(usiUserRef).build();

        domainService.removeManagerFromDomain(usiUser, domain, usiTokenService.aapToken());


        Team team = new Team();
        team.setName(domain.getDomainName());

        return team;
    }
}
