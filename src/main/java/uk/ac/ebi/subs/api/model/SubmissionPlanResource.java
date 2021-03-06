package uk.ac.ebi.subs.api.model;

import org.springframework.hateoas.Resource;
import uk.ac.ebi.subs.repository.model.DataType;
import uk.ac.ebi.subs.repository.model.SubmissionPlan;

import java.util.List;

/**
 * A value object for {@link SubmissionPlan} resource storing the list of data types belongs to the specific submission plan.
 */
public class SubmissionPlanResource extends Resource<SubmissionPlan> {

    public SubmissionPlanResource(Resource<SubmissionPlan> resource) {
        super(resource.getContent(), resource.getLinks());
    }

    private List<DataType> dataTypes;

    public List<DataType> getDataTypes() {
        return dataTypes;
    }

    public void setDataTypes(List<DataType> dataTypes) {
        this.dataTypes = dataTypes;
    }
}
