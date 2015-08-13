package org.ei.opensrp.repository.cloudant;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cloudant.sync.datastore.BasicDocumentRevision;
import com.cloudant.sync.datastore.ConflictException;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DatastoreNotCreatedException;
import com.cloudant.sync.datastore.DocumentBodyFactory;
import com.cloudant.sync.datastore.DocumentException;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.datastore.MutableDocumentRevision;
import com.cloudant.sync.notifications.ReplicationCompleted;
import com.cloudant.sync.notifications.ReplicationErrored;
import com.cloudant.sync.query.IndexManager;
import com.cloudant.sync.query.QueryResult;
import com.cloudant.sync.replication.PullReplication;
import com.cloudant.sync.replication.PushReplication;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorFactory;
import com.fasterxml.jackson.databind.deser.Deserializers;
import com.google.common.eventbus.Subscribe;

import net.sqlcipher.database.SQLiteDatabase;

import org.ei.opensrp.R;
import org.ei.opensrp.domain.Alert;
import org.joda.time.LocalDate;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.StringUtils.repeat;
import static org.ei.drishti.dto.AlertStatus.complete;
import static org.ei.drishti.dto.AlertStatus.from;
import static org.ei.drishti.dto.AlertStatus.inProcess;

/**
 * Created by Geoffrey Koros on 8/6/2015.
 */
public class AlertsModel extends BaseItemsModel{

    private static final String ALERTS_TABLE_NAME = "alerts";
    public static final String ALERTS_CASEID_COLUMN = "caseID";
    public static final String ALERTS_SCHEDULE_NAME_COLUMN = "scheduleName";
    public static final String ALERTS_VISIT_CODE_COLUMN = "visitCode";
    public static final String ALERTS_STATUS_COLUMN = "status";
    public static final String ALERTS_STARTDATE_COLUMN = "startDate";
    public static final String ALERTS_EXPIRYDATE_COLUMN = "expiryDate";
    public static final String ALERTS_COMPLETIONDATE_COLUMN = "completionDate";

    public static final String CASE_AND_VISIT_CODE_COLUMN_SELECTIONS = ALERTS_CASEID_COLUMN + " = ? AND " + ALERTS_VISIT_CODE_COLUMN + " = ?";


    public AlertsModel(Context context) {
        super(context, ALERTS_TABLE_NAME);

        //setup the indexManeger
        if(mIndexManager != null){
            if (mIndexManager.isTextSearchEnabled()) {
                // Create an index over the searchable text fields
                String name = mIndexManager.ensureIndexed(Arrays.<Object>asList(ALERTS_CASEID_COLUMN, ALERTS_SCHEDULE_NAME_COLUMN, ALERTS_VISIT_CODE_COLUMN,
                                ALERTS_STATUS_COLUMN, ALERTS_STARTDATE_COLUMN, ALERTS_EXPIRYDATE_COLUMN, ALERTS_COMPLETIONDATE_COLUMN),
                        "basic");
                if (name == null) {
                    Log.e(LOG_TAG, "there was an error creating the index");
                }
            }else{
                Log.e(LOG_TAG, "there was an error creating the index");
            }
        }
    }

    public List<Alert> allAlerts() {
        int nDocs = this.mDatastore.getDocumentCount();
        List<BasicDocumentRevision> all = this.mDatastore.getAllDocuments(0, nDocs, true);
        List<Alert> alerts = new ArrayList<Alert>();

        // Filter all documents down to those of type Task.
        for(BasicDocumentRevision rev : all) {
            Alert t = Alert.fromRevision(rev);
            if (t != null) {
                alerts.add(t);
            }
        }

        return alerts;
    }

    /**
     * Updates an Alert document within the datastore.
     * @param alert Alert to update
     * @return the updated revision of the Alert
     * @throws ConflictException if the task passed in has a rev which doesn't
     *      match the current rev in the datastore.
     */
    public Alert updateDocument(Alert alert) throws ConflictException {
        MutableDocumentRevision rev = alert.getDocumentRevision().mutableCopy();
        rev.body = DocumentBodyFactory.create(alert.asMap());
        try {
            BasicDocumentRevision updated = this.mDatastore.updateDocumentFromRevision(rev);
            return Alert.fromRevision(updated);
        } catch (DocumentException de) {
            return null;
        }
    }

    public List<Alert> allActiveAlertsForCase(String caseId) {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put(ALERTS_CASEID_COLUMN, caseId);

        List<Alert> alerts = new ArrayList<Alert>();

        QueryResult result = mIndexManager.find(query);
        if(result != null){
            for (DocumentRevision rev : result) {
                if(rev instanceof BasicDocumentRevision){
                    BasicDocumentRevision brev = (BasicDocumentRevision)rev;
                    Alert t = Alert.fromRevision(brev);
                    if (t != null) {
                        alerts.add(t);
                    }
                }
            }
        }

        return filterActiveAlerts(alerts);
    }

    public void createAlert(Alert alert) {
        MutableDocumentRevision rev = new MutableDocumentRevision();
        rev.body = DocumentBodyFactory.create(alert.asMap());
        try {
            BasicDocumentRevision created = this.mDatastore.createDocumentFromRevision(rev);
            Alert.fromRevision(created);
        } catch (DocumentException de) {
            Log.e(LOG_TAG, de.toString());
        }
    }

    public void markAlertAsClosed(String caseId, String visitCode, String completionDate) {
        try {
            Map<String, Object> query = new HashMap<String, Object>();
            query.put(ALERTS_CASEID_COLUMN, caseId);
            query.put(ALERTS_VISIT_CODE_COLUMN, visitCode);

            QueryResult result = mIndexManager.find(query);
            if(result != null){
                for (DocumentRevision rev : result) {
                    if(rev instanceof BasicDocumentRevision){
                        BasicDocumentRevision brev = (BasicDocumentRevision)rev;
                        Alert alert = Alert.fromRevision(brev);
                        alert.setStatus(complete);
                        alert.setCompletionDate(completionDate);
                        updateDocument(alert);
                    }
                }
            }
        } catch (ConflictException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllAlertsForEntity(String caseId) {
        try {
            Map<String, Object> query = new HashMap<String, Object>();
            query.put(ALERTS_CASEID_COLUMN, caseId);

            QueryResult result = mIndexManager.find(query);
            if(result != null){
                for (DocumentRevision rev : result) {
                    if(rev instanceof BasicDocumentRevision){
                        BasicDocumentRevision brev = (BasicDocumentRevision)rev;
                        Alert alert = Alert.fromRevision(brev);
                        alert.setStatus(complete);
                        deleteDocument(alert);
                    }
                }
            }
        } catch (ConflictException e) {
            e.printStackTrace();
        }
    }

    //TODO:
    public void deleteAllAlerts() {
        try {
            int nDocs = this.mDatastore.getDocumentCount();
            List<BasicDocumentRevision> all = this.mDatastore.getAllDocuments(0, nDocs, true);

            // Filter all documents down to those of type Task.
            for(BasicDocumentRevision rev : all) {
                Alert t = Alert.fromRevision(rev);
                if (t != null) {
                    deleteDocument(t);
                }
            }
        } catch (ConflictException e) {
            e.printStackTrace();
        }
    }

    private List<Alert> readAllAlerts(Cursor cursor) {
        cursor.moveToFirst();
        List<Alert> alerts = new ArrayList<Alert>();
        while (!cursor.isAfterLast()) {
            alerts.add(
                    new Alert(cursor.getString(cursor.getColumnIndex(ALERTS_CASEID_COLUMN)),
                            cursor.getString(cursor.getColumnIndex(ALERTS_SCHEDULE_NAME_COLUMN)), cursor.getString(cursor.getColumnIndex(ALERTS_VISIT_CODE_COLUMN)),
                            from(cursor.getString(cursor.getColumnIndex(ALERTS_STATUS_COLUMN))),
                            cursor.getString(cursor.getColumnIndex(ALERTS_STARTDATE_COLUMN)),
                            cursor.getString(cursor.getColumnIndex(ALERTS_EXPIRYDATE_COLUMN))
                    )
                            .withCompletionDate(cursor.getString(cursor.getColumnIndex(ALERTS_COMPLETIONDATE_COLUMN))));
            cursor.moveToNext();
        }
        cursor.close();
        return alerts;
    }

    private List<Alert> filterActiveAlerts(List<Alert> alerts) {
        List<Alert> activeAlerts = new ArrayList<Alert>();
        for (Alert alert : alerts) {
            LocalDate today = LocalDate.now();
            if (LocalDate.parse(alert.expiryDate()).isAfter(today) || (complete.equals(alert.status()) && LocalDate.parse(alert.completionDate()).isAfter(today.minusDays(3)))) {
                activeAlerts.add(alert);
            }
        }
        return activeAlerts;
    }

    private ContentValues createValuesFor(Alert alert) {
        ContentValues values = new ContentValues();
        values.put(ALERTS_CASEID_COLUMN, alert.caseId());
        values.put(ALERTS_SCHEDULE_NAME_COLUMN, alert.scheduleName());
        values.put(ALERTS_VISIT_CODE_COLUMN, alert.visitCode());
        values.put(ALERTS_STATUS_COLUMN, alert.status().value());
        values.put(ALERTS_STARTDATE_COLUMN, alert.startDate());
        values.put(ALERTS_EXPIRYDATE_COLUMN, alert.expiryDate());
        values.put(ALERTS_COMPLETIONDATE_COLUMN, alert.completionDate());
        return values;
    }

    //TODO: complex querying required will revisit this later
    public List<Alert> findByEntityIdAndAlertNames(String entityId, String... names) {
        int nDocs = this.mDatastore.getDocumentCount();
        List<BasicDocumentRevision> all = this.mDatastore.getAllDocuments(0, nDocs, true);
        List<Alert> alerts = new ArrayList<Alert>();

        // Filter all documents down to those of type Task.
        for(BasicDocumentRevision rev : all) {
            Alert t = Alert.fromRevision(rev);
            if (t != null) {
                alerts.add(t);
            }
        }

        return alerts;
    }

    private String insertPlaceholdersForInClause(int length) {
        return repeat("?", ",", length);
    }

    public void changeAlertStatusToInProcess(String entityId, String alertName) {
        try {
            Map<String, Object> query = new HashMap<String, Object>();
            query.put(ALERTS_CASEID_COLUMN, entityId);
            query.put(ALERTS_VISIT_CODE_COLUMN, alertName);

            QueryResult result = mIndexManager.find(query);
            if(result != null){
                for (DocumentRevision rev : result) {
                    if(rev instanceof BasicDocumentRevision){
                        BasicDocumentRevision brev = (BasicDocumentRevision)rev;
                        Alert alert = Alert.fromRevision(brev);
                        alert.setStatus(inProcess);
                        updateDocument(alert);
                    }
                }
            }
        } catch (ConflictException e) {
            e.printStackTrace();
        }
    }

    /**
     * Deletes a Alert document within the datastore.
     * @param alert Alert to delete
     * @throws ConflictException if the task passed in has a rev which doesn't
     *      match the current rev in the datastore.
     */
    public void deleteDocument(Alert alert) throws ConflictException {
        this.mDatastore.deleteDocumentFromRevision(alert.getDocumentRevision()); //We should have db column rather than actualy deleting the entity
    }

    @Override
    public String getCloudantApiKey() {
        return mContext.getString(R.string.default_api_key);
    }

    @Override
    public String getCloudantDatabaseName() {
        return mContext.getString(R.string.alerts_dbname);
    }

    @Override
    public String getCloudantApiSecret() {
        return mContext.getString(R.string.default_api_password);
    }
}
