package org.ei.opensrp.service;

import org.ei.opensrp.AllConstants;
import org.ei.opensrp.application.OpenSRPApplication;
import org.ei.opensrp.db.adapters.AlertRepository;
import org.ei.opensrp.db.adapters.BeneficiariesAdapter;
import org.ei.opensrp.db.adapters.ChildRepository;
import org.ei.opensrp.db.adapters.MotherRepository;
import org.ei.opensrp.db.adapters.TimelineEventRepository;
import org.ei.opensrp.domain.Child;
import org.ei.opensrp.domain.Mother;
import org.ei.opensrp.domain.ServiceProvided;
import org.ei.opensrp.domain.form.FormSubmission;
import org.ei.opensrp.domain.form.SubForm;
import org.ei.opensrp.util.EasyMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.*;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.CHILD_REFERRAL;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.CHILD_SIGNS;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.CHILD_SIGNS_OTHER;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.REPORT_CHILD_DISEASE;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.REPORT_CHILD_DISEASE_DATE;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.REPORT_CHILD_DISEASE_OTHER;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.REPORT_CHILD_DISEASE_PLACE;
import static org.ei.opensrp.AllConstants.ChildIllnessFields.SICK_VISIT_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.*;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.BCG_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.DPTBOOSTER_1_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.DPTBOOSTER_2_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.HEPB_BIRTH_DOSE_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.JE_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.MEASLESBOOSTER_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.MEASLES_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.MMR_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.OPVBOOSTER_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.OPV_0_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.OPV_1_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.OPV_2_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.OPV_3_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.PENTAVALENT_1_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.PENTAVALENT_2_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.PENTAVALENT_3_DATE;
import static org.ei.opensrp.AllConstants.ChildRegistrationECFields.SHOULD_CLOSE_MOTHER;
import static org.ei.opensrp.AllConstants.ChildRegistrationOAFields.CHILD_ID;
import static org.ei.opensrp.AllConstants.ChildRegistrationOAFields.THAYI_CARD_NUMBER;
import static org.ei.opensrp.AllConstants.ENTITY_ID_FIELD_NAME;
import static org.ei.opensrp.AllConstants.Immunizations.*;
import static org.ei.opensrp.AllConstants.Immunizations.BCG;
import static org.ei.opensrp.AllConstants.Immunizations.DPT_BOOSTER_1;
import static org.ei.opensrp.AllConstants.Immunizations.DPT_BOOSTER_2;
import static org.ei.opensrp.AllConstants.Immunizations.HEPATITIS_BIRTH_DOSE;
import static org.ei.opensrp.AllConstants.Immunizations.JE;
import static org.ei.opensrp.AllConstants.Immunizations.MEASLES;
import static org.ei.opensrp.AllConstants.Immunizations.MEASLES_BOOSTER;
import static org.ei.opensrp.AllConstants.Immunizations.MMR;
import static org.ei.opensrp.AllConstants.Immunizations.OPV_0;
import static org.ei.opensrp.AllConstants.Immunizations.OPV_1;
import static org.ei.opensrp.AllConstants.Immunizations.OPV_2;
import static org.ei.opensrp.AllConstants.Immunizations.OPV_3;
import static org.ei.opensrp.AllConstants.Immunizations.OPV_BOOSTER;
import static org.ei.opensrp.AllConstants.Immunizations.PENTAVALENT_1;
import static org.ei.opensrp.AllConstants.Immunizations.PENTAVALENT_2;
import static org.ei.opensrp.AllConstants.Immunizations.PENTAVALENT_3;
import static org.ei.opensrp.AllConstants.SPACE;
import static org.ei.opensrp.domain.TimelineEvent.forChildBirthInChildProfile;
import static org.ei.opensrp.domain.TimelineEvent.forChildBirthInECProfile;
import static org.ei.opensrp.domain.TimelineEvent.forChildBirthInMotherProfile;
import static org.ei.opensrp.domain.TimelineEvent.forChildImmunization;
import static org.ei.opensrp.domain.TimelineEvent.forChildPNCVisit;

public class ChildService {
    @Inject
    private BeneficiariesAdapter allBeneficiaries;

    @Inject
    private ChildRepository childRepository;

    @Inject
    private MotherRepository motherRepository;

    @Inject
    private TimelineEventRepository allTimelines;

    @Inject
    private ServiceProvidedService serviceProvidedService;

    @Inject
    private AlertRepository allAlerts;

    public ChildService() {
        OpenSRPApplication.getInstance().inject(this);
    }

    public void register(FormSubmission submission) {
        SubForm subForm = submission.getSubFormByName(AllConstants.DeliveryOutcomeFields.CHILD_REGISTRATION_SUB_FORM_NAME);
        if (handleStillBirth(submission, subForm)) return;
        String referenceDate = submission.getFieldValue(AllConstants.DeliveryOutcomeFields.REFERENCE_DATE);
        String deliveryPlace = submission.getFieldValue(AllConstants.DeliveryOutcomeFields.DELIVERY_PLACE);
        Mother mother = motherRepository.findById(submission.entityId());
        for (Map<String, String> childInstance : subForm.instances()) {
            Child child = childRepository.find(childInstance.get(ENTITY_ID_FIELD_NAME));
            childRepository.update(child.setIsClosed(false).setThayiCardNumber(mother.thayiCardNumber()).setDateOfBirth(referenceDate));
            allTimelines.add(forChildBirthInChildProfile(child.caseId(), referenceDate,
                    child.getDetail(AllConstants.ChildRegistrationFields.WEIGHT), child.getDetail(AllConstants.ChildRegistrationFields.IMMUNIZATIONS_GIVEN)));
            allTimelines.add(forChildBirthInMotherProfile(mother.caseId(), referenceDate, child.gender(), referenceDate, deliveryPlace));
            allTimelines.add(forChildBirthInECProfile(mother.ecCaseId(), referenceDate, child.gender(), referenceDate));
            String immunizationsGiven = child.getDetail(AllConstants.ChildRegistrationFields.IMMUNIZATIONS_GIVEN);
            for (String immunization : immunizationsGiven.split(SPACE)) {
                serviceProvidedService.add(ServiceProvided.forChildImmunization(child.caseId(), immunization, referenceDate));
            }
        }
    }

    private boolean isDeliveryOutcomeStillBirth(FormSubmission submission) {
        return AllConstants.DeliveryOutcomeFields.STILL_BIRTH_VALUE
                .equalsIgnoreCase(submission.getFieldValue(AllConstants.DeliveryOutcomeFields.DELIVERY_OUTCOME));
    }

    public void registerForEC(FormSubmission submission) {
        if (shouldCloseMother(submission.getFieldValue(SHOULD_CLOSE_MOTHER))) {
            closeMother(submission.getFieldValue(AllConstants.ChildRegistrationFields.MOTHER_ID));
        }

        Map<String, String> immunizationDateFieldMap = createImmunizationDateFieldMap();
        allTimelines.add(forChildBirthInChildProfile(
                submission.getFieldValue(AllConstants.ChildRegistrationFields.CHILD_ID),
                submission.getFieldValue(AllConstants.ChildRegistrationFields.DATE_OF_BIRTH),
                submission.getFieldValue(AllConstants.ChildRegistrationFields.WEIGHT),
                submission.getFieldValue(AllConstants.ChildRegistrationFields.IMMUNIZATIONS_GIVEN)));
        allTimelines.add(forChildBirthInMotherProfile(
                submission.getFieldValue(AllConstants.ChildRegistrationFields.MOTHER_ID),
                submission.getFieldValue(AllConstants.ChildRegistrationFields.DATE_OF_BIRTH),
                submission.getFieldValue(AllConstants.ChildRegistrationFields.GENDER),
                null, null));
        allTimelines.add(forChildBirthInECProfile(
                submission.entityId(),
                submission.getFieldValue(AllConstants.ChildRegistrationFields.DATE_OF_BIRTH),
                submission.getFieldValue(AllConstants.ChildRegistrationFields.GENDER), null));
        String immunizationsGiven = submission.getFieldValue(AllConstants.ChildRegistrationFields.IMMUNIZATIONS_GIVEN);
        for (String immunization : immunizationsGiven.split(SPACE)) {
            serviceProvidedService.add(ServiceProvided.forChildImmunization(
                    submission.getFieldValue(AllConstants.ChildRegistrationFields.CHILD_ID),
                    immunization,
                    submission.getFieldValue(immunizationDateFieldMap.get(immunization))));
        }
    }

    private void closeMother(String id) {
        Mother mother = allBeneficiaries.findMother(id);
        mother.setIsClosed(true);
        allBeneficiaries.updateMother(mother);
    }

    private boolean shouldCloseMother(String shouldCloseMother) {
        return isBlank(shouldCloseMother) || Boolean.parseBoolean(shouldCloseMother);
    }

    private Map<String, String> createImmunizationDateFieldMap() {
        Map<String, String> immunizationDateFieldsMap = new HashMap<String, String>();
        immunizationDateFieldsMap.put(BCG, BCG_DATE);

        immunizationDateFieldsMap.put(MEASLES, MEASLES_DATE);
        immunizationDateFieldsMap.put(MEASLES_BOOSTER, MEASLESBOOSTER_DATE);

        immunizationDateFieldsMap.put(OPV_0, OPV_0_DATE);
        immunizationDateFieldsMap.put(OPV_1, OPV_1_DATE);
        immunizationDateFieldsMap.put(OPV_2, OPV_2_DATE);
        immunizationDateFieldsMap.put(OPV_3, OPV_3_DATE);
        immunizationDateFieldsMap.put(OPV_BOOSTER, OPVBOOSTER_DATE);

        immunizationDateFieldsMap.put(DPT_BOOSTER_1, DPTBOOSTER_1_DATE);
        immunizationDateFieldsMap.put(DPT_BOOSTER_2, DPTBOOSTER_2_DATE);

        immunizationDateFieldsMap.put(HEPATITIS_BIRTH_DOSE, HEPB_BIRTH_DOSE_DATE);

        immunizationDateFieldsMap.put(PENTAVALENT_1, PENTAVALENT_1_DATE);
        immunizationDateFieldsMap.put(PENTAVALENT_2, PENTAVALENT_2_DATE);
        immunizationDateFieldsMap.put(PENTAVALENT_3, PENTAVALENT_3_DATE);

        immunizationDateFieldsMap.put(MMR, MMR_DATE);
        immunizationDateFieldsMap.put(JE, JE_DATE);

        return immunizationDateFieldsMap;
    }

    public void pncRegistrationOA(FormSubmission submission) {
        SubForm subForm = submission.getSubFormByName(AllConstants.PNCRegistrationOAFields.CHILD_REGISTRATION_OA_SUB_FORM_NAME);
        if (handleStillBirth(submission, subForm)) return;

        String referenceDate = submission.getFieldValue(AllConstants.DeliveryOutcomeFields.REFERENCE_DATE);
        String deliveryPlace = submission.getFieldValue(AllConstants.DeliveryOutcomeFields.DELIVERY_PLACE);
        Mother mother = motherRepository.findAllCasesForEC(submission.entityId()).get(0);

        for (Map<String, String> childInstances : subForm.instances()) {
            Child child = childRepository.find(childInstances.get(ENTITY_ID_FIELD_NAME));
            childRepository.update(child.setIsClosed(false).setThayiCardNumber(mother.thayiCardNumber()).setDateOfBirth(referenceDate));
            allTimelines.add(forChildBirthInChildProfile(child.caseId(), referenceDate,
                    child.getDetail(AllConstants.PNCRegistrationOAFields.WEIGHT), child.getDetail(AllConstants.PNCRegistrationOAFields.IMMUNIZATIONS_GIVEN)));
            allTimelines.add(forChildBirthInMotherProfile(mother.caseId(), referenceDate, child.gender(),
                    referenceDate, deliveryPlace));
            allTimelines.add(forChildBirthInECProfile(mother.ecCaseId(), referenceDate, child.gender(),
                    referenceDate));
            String immunizationsGiven = child.getDetail(AllConstants.PNCRegistrationOAFields.IMMUNIZATIONS_GIVEN);
            for (String immunization : immunizationsGiven.split(SPACE)) {
                serviceProvidedService.add(ServiceProvided.forChildImmunization(child.caseId(), immunization, referenceDate));
            }
        }
    }

    private boolean handleStillBirth(FormSubmission submission, SubForm subForm) {
        if (!isDeliveryOutcomeStillBirth(submission)) {
            return false;
        }
        if (!subForm.instances().isEmpty()) {
            String childId = subForm.instances().get(0).get(ENTITY_ID_FIELD_NAME);
            childRepository.delete(childId);
        }
        return true;
    }

    public void updateImmunizations(FormSubmission submission) {
        String immunizationDate = submission.getFieldValue(AllConstants.ChildImmunizationsFields.IMMUNIZATION_DATE);
        List<String> immunizationsGivenList = splitFieldValueBySpace(submission, AllConstants.ChildImmunizationsFields.IMMUNIZATIONS_GIVEN);
        List<String> previousImmunizationsList = splitFieldValueBySpace(submission, AllConstants.ChildImmunizationsFields.PREVIOUS_IMMUNIZATIONS_GIVEN);
        immunizationsGivenList.removeAll(previousImmunizationsList);
        for (String immunization : immunizationsGivenList) {
            allTimelines.add(forChildImmunization(submission.entityId(), immunization, immunizationDate));
            serviceProvidedService.add(ServiceProvided.forChildImmunization(submission.entityId(), immunization, immunizationDate));
            allAlerts.changeAlertStatusToInProcess(submission.entityId(), immunization);
        }
    }

    private ArrayList<String> splitFieldValueBySpace(FormSubmission submission, String fieldName) {
        return new ArrayList<String>(Arrays.asList(submission.getFieldValue(fieldName).split(SPACE)));
    }

    public void pncVisitHappened(FormSubmission submission) {
        String pncVisitDate = submission.getFieldValue(AllConstants.PNCVisitFields.PNC_VISIT_DATE);
        String pncVisitDay = submission.getFieldValue(AllConstants.PNCVisitFields.PNC_VISIT_DAY);
        SubForm subForm = submission.getSubFormByName(AllConstants.PNCVisitFields.CHILD_PNC_VISIT_SUB_FORM_NAME);

        if (handleStillBirth(submission, subForm)) return;

        for (Map<String, String> childInstances : subForm.instances()) {
            allTimelines.add(forChildPNCVisit(childInstances.get(ENTITY_ID_FIELD_NAME), pncVisitDay,
                    pncVisitDate, childInstances.get(AllConstants.PNCVisitFields.WEIGHT), childInstances.get(AllConstants.PNCVisitFields.TEMPERATURE)));
            serviceProvidedService.add(ServiceProvided.forChildPNCVisit(childInstances.get(ENTITY_ID_FIELD_NAME), pncVisitDay, pncVisitDate));
        }
    }

    public void close(FormSubmission submission) {
        allBeneficiaries.closeChild(submission.entityId());
    }

    public void updatePhotoPath(String entityId, String imagePath) {
        childRepository.updatePhotoPath(entityId, imagePath);
    }

    public void updateIllnessStatus(FormSubmission submission) {
        String sickVisitDate = submission.getFieldValue(SICK_VISIT_DATE);
        String date = sickVisitDate != null ?
                sickVisitDate : submission.getFieldValue(REPORT_CHILD_DISEASE_DATE);
        serviceProvidedService.add(
                ServiceProvided.forChildIllnessVisit(submission.entityId(),
                        date,
                        createChildIllnessMap(submission))
        );
    }

    private Map<String, String> createChildIllnessMap(FormSubmission submission) {
        return EasyMap.create(CHILD_SIGNS, submission.getFieldValue(CHILD_SIGNS))
                .put(CHILD_SIGNS_OTHER, submission.getFieldValue(CHILD_SIGNS_OTHER))
                .put(SICK_VISIT_DATE, submission.getFieldValue(SICK_VISIT_DATE))
                .put(REPORT_CHILD_DISEASE, submission.getFieldValue(REPORT_CHILD_DISEASE))
                .put(REPORT_CHILD_DISEASE_OTHER, submission.getFieldValue(REPORT_CHILD_DISEASE_OTHER))
                .put(REPORT_CHILD_DISEASE_DATE, submission.getFieldValue(REPORT_CHILD_DISEASE_DATE))
                .put(REPORT_CHILD_DISEASE_PLACE, submission.getFieldValue(REPORT_CHILD_DISEASE_PLACE))
                .put(CHILD_REFERRAL, submission.getFieldValue(CHILD_REFERRAL))
                .map();
    }

    public void updateVitaminAProvided(FormSubmission submission) {
        serviceProvidedService.add(
                ServiceProvided.forVitaminAProvided(submission.entityId(),
                        submission.getFieldValue(AllConstants.VitaminAFields.VITAMIN_A_DATE),
                        submission.getFieldValue(AllConstants.VitaminAFields.VITAMIN_A_DOSE),
                        submission.getFieldValue(AllConstants.VitaminAFields.VITAMIN_A_PLACE)));
    }

    public void registerForOA(FormSubmission submission) {
        Child child = allBeneficiaries.findChild(submission.getFieldValue(CHILD_ID));
        child.setThayiCardNumber(submission.getFieldValue(THAYI_CARD_NUMBER));
        allBeneficiaries.updateChild(child);

        Map<String, String> immunizationDateFieldMap = createImmunizationDateFieldMap();

        String immunizationsGiven = submission.getFieldValue(AllConstants.ChildRegistrationOAFields.IMMUNIZATIONS_GIVEN);
        immunizationsGiven = isBlank(immunizationsGiven) ? "" : immunizationsGiven;
        allTimelines.add(forChildBirthInChildProfile(
                submission.getFieldValue(CHILD_ID),
                submission.getFieldValue(AllConstants.ChildRegistrationOAFields.DATE_OF_BIRTH),
                submission.getFieldValue(AllConstants.ChildRegistrationOAFields.WEIGHT),
                immunizationsGiven));

        for (String immunization : immunizationsGiven.split(SPACE)) {
            serviceProvidedService.add(ServiceProvided.forChildImmunization(submission.entityId(), immunization, submission.getFieldValue(immunizationDateFieldMap.get(immunization))));
        }
    }
}
