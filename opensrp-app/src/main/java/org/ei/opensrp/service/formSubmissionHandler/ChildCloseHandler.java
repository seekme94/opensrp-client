package org.ei.opensrp.service.formSubmissionHandler;

import org.ei.opensrp.domain.form.FormSubmission;
import org.ei.opensrp.service.ChildService;

import javax.inject.Inject;

public class ChildCloseHandler implements FormSubmissionHandler {

    @Inject
    private ChildService childService;

    @Override
    public void handle(FormSubmission submission) {
        childService.close(submission);
    }
}
