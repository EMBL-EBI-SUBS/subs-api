package uk.ac.ebi.subs.api.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.stereotype.Service;
import uk.ac.ebi.subs.messaging.Exchanges;
import uk.ac.ebi.subs.messaging.Topics;
import uk.ac.ebi.subs.processing.SubmissionEnvelope;
import uk.ac.ebi.subs.repository.model.Submission;

/**
 * This is a Spring @Service component for {@link uk.ac.ebi.subs.repository.model.Submission} entity.
 * Send a submission off to the RabbitMQ exchange for processing it.
 */
@Service
public class SubmissionEventServiceImpl implements SubmissionEventService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private RabbitMessagingTemplate rabbitMessagingTemplate;

    @Autowired
    public SubmissionEventServiceImpl(RabbitMessagingTemplate rabbitMessagingTemplate, MessageConverter messageConverter) {
        this.rabbitMessagingTemplate = rabbitMessagingTemplate;
        this.rabbitMessagingTemplate.setMessageConverter(messageConverter);
    }

    @Override
    public void submissionCreated(Submission submission) {
        //TODO
    }

    @Override
    public void submissionUpdated(Submission submission) {
        //TODO
    }

    @Override
    public void submissionSubmitted(SubmissionEnvelope submissionEnvelope) {

        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                Topics.EVENT_SUBMISSION_SUBMITTED,
                submissionEnvelope
        );

        logger.info("Sent submission {} to message broker with '{}' routing key.",
                submissionEnvelope.getSubmission().getId(), Topics.EVENT_SUBMISSION_SUBMITTED);
    }

    @Override
    public void submissionDeleted(Submission submission) {
        rabbitMessagingTemplate.convertAndSend(
                Exchanges.SUBMISSIONS,
                Topics.EVENT_SUBMISSION_DELETED,
                submission
        );

        logger.warn("sent submission {}", submission.getId());
    }
}
