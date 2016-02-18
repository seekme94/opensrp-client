package org.ei.opensrp.service.formSubmissionHandler;

import android.webkit.JavascriptInterface;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ei.opensrp.application.OpenSRPApplication;
import org.ei.opensrp.db.adapters.FormDataRepository;
import org.ei.opensrp.domain.form.FormSubmission;
import org.ei.opensrp.util.Log;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import static java.text.MessageFormat.format;
import static org.ei.opensrp.AllConstants.FormNames.ANC_CLOSE;
import static org.ei.opensrp.AllConstants.FormNames.ANC_INVESTIGATIONS;
import static org.ei.opensrp.AllConstants.FormNames.ANC_REGISTRATION;
import static org.ei.opensrp.AllConstants.FormNames.ANC_REGISTRATION_OA;
import static org.ei.opensrp.AllConstants.FormNames.ANC_VISIT;
import static org.ei.opensrp.AllConstants.FormNames.CHILD_CLOSE;
import static org.ei.opensrp.AllConstants.FormNames.CHILD_ILLNESS;
import static org.ei.opensrp.AllConstants.FormNames.CHILD_IMMUNIZATIONS;
import static org.ei.opensrp.AllConstants.FormNames.CHILD_REGISTRATION_EC;
import static org.ei.opensrp.AllConstants.FormNames.CHILD_REGISTRATION_OA;
import static org.ei.opensrp.AllConstants.FormNames.DELIVERY_OUTCOME;
import static org.ei.opensrp.AllConstants.FormNames.DELIVERY_PLAN;
import static org.ei.opensrp.AllConstants.FormNames.EC_CLOSE;
import static org.ei.opensrp.AllConstants.FormNames.EC_EDIT;
import static org.ei.opensrp.AllConstants.FormNames.EC_REGISTRATION;
import static org.ei.opensrp.AllConstants.FormNames.FP_CHANGE;
import static org.ei.opensrp.AllConstants.FormNames.FP_COMPLICATIONS;
import static org.ei.opensrp.AllConstants.FormNames.HB_TEST;
import static org.ei.opensrp.AllConstants.FormNames.IFA;
import static org.ei.opensrp.AllConstants.FormNames.PNC_CLOSE;
import static org.ei.opensrp.AllConstants.FormNames.PNC_REGISTRATION_OA;
import static org.ei.opensrp.AllConstants.FormNames.PNC_VISIT;
import static org.ei.opensrp.AllConstants.FormNames.RENEW_FP_PRODUCT;
import static org.ei.opensrp.AllConstants.FormNames.TT;
import static org.ei.opensrp.AllConstants.FormNames.TT_1;
import static org.ei.opensrp.AllConstants.FormNames.TT_2;
import static org.ei.opensrp.AllConstants.FormNames.TT_BOOSTER;
import static org.ei.opensrp.AllConstants.FormNames.VITAMIN_A;
import static org.ei.opensrp.event.Event.FORM_SUBMITTED;
import static org.ei.opensrp.util.Log.logWarn;


public class FormSubmissionRouter {
    private final Map<String, FormSubmissionHandler> handlerMap;

    @Inject
    private FormDataRepository formDataRepository;

    @Inject
    ECRegistrationHandler ecRegistrationHandler;

    @Inject
    FPComplicationsHandler fpComplicationsHandler;

    @Inject
    FPChangeHandler fpChangeHandler;

    @Inject
    RenewFPProductHandler renewFPProductHandler;

    @Inject
    ECCloseHandler ecCloseHandler;

    @Inject
    ANCRegistrationHandler ancRegistrationHandler;

    @Inject
    ANCRegistrationOAHandler ancRegistrationOAHandler;

    @Inject
    ANCVisitHandler ancVisitHandler;

    @Inject
    ANCCloseHandler ancCloseHandler;

    @Inject
    TTHandler ttHandler;

    @Inject
    IFAHandler ifaHandler;

    @Inject
    HBTestHandler hbTestHandler;

    @Inject
    DeliveryOutcomeHandler deliveryOutcomeHandler;

    @Inject
    PNCRegistrationOAHandler pncRegistrationOAHandler;

    @Inject
    PNCCloseHandler pncCloseHandler;

    @Inject
    PNCVisitHandler pncVisitHandler;

    @Inject
    ChildImmunizationsHandler childImmunizationsHandler;

    @Inject
    ChildRegistrationECHandler childRegistrationECHandler;

    @Inject
    ChildRegistrationOAHandler childRegistrationOAHandler;

    @Inject
    ChildCloseHandler childCloseHandler;

    @Inject
    ChildIllnessHandler childIllnessHandler;

    @Inject
    VitaminAHandler vitaminAHandler;

    @Inject
    DeliveryPlanHandler deliveryPlanHandler;

    @Inject
    ECEditHandler ecEditHandler;

    @Inject
    ANCInvestigationsHandler ancInvestigationsHandler;

    public FormSubmissionRouter() {
        OpenSRPApplication.getInstance().inject(this);
        handlerMap = new HashMap<String, FormSubmissionHandler>();
        handlerMap.put(EC_REGISTRATION, ecRegistrationHandler);
        handlerMap.put(FP_COMPLICATIONS, fpComplicationsHandler);
        handlerMap.put(FP_CHANGE, fpChangeHandler);
        handlerMap.put(RENEW_FP_PRODUCT, renewFPProductHandler);
        handlerMap.put(EC_CLOSE, ecCloseHandler);
        handlerMap.put(ANC_REGISTRATION, ancRegistrationHandler);
        handlerMap.put(ANC_REGISTRATION_OA, ancRegistrationOAHandler);
        handlerMap.put(ANC_VISIT, ancVisitHandler);
        handlerMap.put(ANC_CLOSE, ancCloseHandler);
        handlerMap.put(TT, ttHandler);
        handlerMap.put(TT_BOOSTER, ttHandler);
        handlerMap.put(TT_1, ttHandler);
        handlerMap.put(TT_2, ttHandler);
        handlerMap.put(IFA, ifaHandler);
        handlerMap.put(HB_TEST, hbTestHandler);
        handlerMap.put(DELIVERY_OUTCOME, deliveryOutcomeHandler);
        handlerMap.put(PNC_REGISTRATION_OA, pncRegistrationOAHandler);
        handlerMap.put(PNC_CLOSE, pncCloseHandler);
        handlerMap.put(PNC_VISIT, pncVisitHandler);
        handlerMap.put(CHILD_IMMUNIZATIONS, childImmunizationsHandler);
        handlerMap.put(CHILD_REGISTRATION_EC, childRegistrationECHandler);
        handlerMap.put(CHILD_REGISTRATION_OA, childRegistrationOAHandler);
        handlerMap.put(CHILD_CLOSE, childCloseHandler);
        handlerMap.put(CHILD_ILLNESS, childIllnessHandler);
        handlerMap.put(VITAMIN_A, vitaminAHandler);
        handlerMap.put(DELIVERY_PLAN, deliveryPlanHandler);
        handlerMap.put(EC_EDIT, ecEditHandler);
        handlerMap.put(ANC_INVESTIGATIONS, ancInvestigationsHandler);
    }

    @JavascriptInterface
    public void route(String instanceId) throws Exception {
        FormSubmission submission = formDataRepository.fetchFromSubmission(instanceId);
        FormSubmissionHandler handler = handlerMap.get(submission.formName());
        if (handler == null) {
            logWarn("Could not find a handler due to unknown form submission: " + submission);
        } else {
            try {
                handler.handle(submission);
            } catch (Exception e) {
                Log.logError(format("Handling {0} form submission with instance Id: {1} for entity: {2} failed with exception : {3}",
                        submission.formName(), submission.instanceId(), submission.entityId(), ExceptionUtils.getStackTrace(e)));
                throw e;
            }
        }
        FORM_SUBMITTED.notifyListeners(instanceId);
    }

    public Map<String, FormSubmissionHandler> getHandlerMap() {
        return handlerMap;
    }
}
