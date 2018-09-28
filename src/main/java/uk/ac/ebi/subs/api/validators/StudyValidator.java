package uk.ac.ebi.subs.api.validators;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import uk.ac.ebi.subs.repository.model.Study;
import uk.ac.ebi.subs.repository.repos.submittables.StudyRepository;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class StudyValidator implements Validator {


    public StudyValidator(CoreSubmittableValidationHelper coreSubmittableValidationHelper, StudyRepository studyRepository) {
        this.coreSubmittableValidationHelper = coreSubmittableValidationHelper;
        this.studyRepository = studyRepository;
    }

    private CoreSubmittableValidationHelper coreSubmittableValidationHelper;
    private StudyRepository studyRepository;


    @Override
    public boolean supports(Class<?> clazz) {
        return Study.class.isAssignableFrom(clazz);
    }

    @Override
    public void validate(Object target, Errors errors) {
        Study study = (Study) target;
        coreSubmittableValidationHelper.validate(study, studyRepository, errors);
    }
}

