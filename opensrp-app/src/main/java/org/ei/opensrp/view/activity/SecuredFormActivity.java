package org.ei.opensrp.view.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.webkit.GeolocationPermissions;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;

import org.apache.commons.io.IOUtils;
import org.ei.opensrp.Context;
import org.ei.opensrp.db.adapters.FormDataRepository;
import org.ei.opensrp.service.ZiggyFileLoader;
import org.ei.opensrp.service.formSubmissionHandler.FormSubmissionRouter;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.MessageFormat;

import javax.inject.Inject;

import static java.util.UUID.randomUUID;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.ei.opensrp.AllConstants.ENTITY_ID_PARAM;
import static org.ei.opensrp.AllConstants.FIELD_OVERRIDES_PARAM;
import static org.ei.opensrp.AllConstants.FORM_NAME_PARAM;
import static org.ei.opensrp.AllConstants.FORM_SUBMISSION_ROUTER;
import static org.ei.opensrp.AllConstants.INSTANCE_ID_PARAM;
import static org.ei.opensrp.AllConstants.REPOSITORY;
import static org.ei.opensrp.AllConstants.ZIGGY_FILE_LOADER;
import static org.ei.opensrp.R.string.form_back_confirm_dialog_message;
import static org.ei.opensrp.R.string.form_back_confirm_dialog_title;
import static org.ei.opensrp.R.string.no_button_label;
import static org.ei.opensrp.R.string.yes_button_label;
import static org.ei.opensrp.util.Log.logError;

public abstract class SecuredFormActivity extends SecuredWebActivity {
    public static final String ANDROID_CONTEXT_FIELD = "androidContext";
    private String model;
    private String form;
    private String formName;
    private String entityId;
    private String fieldOverrides;

    @Inject
    FormDataRepository formDataRepository;

    @Inject
    ZiggyFileLoader ziggyFileLoader;

    @Inject
    FormSubmissionRouter formSubmissionRouter;

    public SecuredFormActivity() {
        super();
        shouldDismissProgressBarOnProgressComplete = false;
    }

    @Override
    protected void onInitialization() {
        try {
            getIntentData();
        } catch (IOException e) {
            logError(e.toString());
            finish();
        }
        webViewInitialization();
    }

    private void getIntentData() throws IOException {
        Intent intent = getIntent();
        formName = intent.getStringExtra(FORM_NAME_PARAM);
        entityId = intent.getStringExtra(ENTITY_ID_PARAM);
        fieldOverrides = intent.getStringExtra(FIELD_OVERRIDES_PARAM);
        model = IOUtils.toString(getAssets().open("www/form/" + formName + "/model.xml"));
        form = IOUtils.toString(getAssets().open("www/form/" + formName + "/form.xml"));
    }

    private void webViewInitialization() {
        WebSettings webViewSettings = webView.getSettings();
        webViewSettings.setJavaScriptEnabled(true);
        webViewSettings.setGeolocationEnabled(true);
        webView.setWebChromeClient(new GeoWebChromeClient());
        webViewSettings.setDatabaseEnabled(true);
        webViewSettings.setDomStorageEnabled(true);
        webView.addJavascriptInterface(new FormWebInterface(model, form, this), ANDROID_CONTEXT_FIELD);
        webView.addJavascriptInterface(formDataRepository, REPOSITORY);
        webView.addJavascriptInterface(ziggyFileLoader, ZIGGY_FILE_LOADER);
        webView.addJavascriptInterface(formSubmissionRouter, FORM_SUBMISSION_ROUTER);
        String encodedFieldOverrides = null;
        try {
            if (isNotBlank(this.fieldOverrides)) {
                encodedFieldOverrides = URLEncoder.encode(this.fieldOverrides, "utf-8");
            }
        } catch (Exception e) {
            logError(MessageFormat.format("Cannot encode field overrides: {0} due to : {1}", fieldOverrides, e));
        }
        webView.loadUrl(MessageFormat.format("file:///android_asset/www/enketo/template.html?{0}={1}&{2}={3}&{4}={5}&{6}={7}&touch=true",
                FORM_NAME_PARAM, formName,
                ENTITY_ID_PARAM, entityId,
                INSTANCE_ID_PARAM, randomUUID(),
                FIELD_OVERRIDES_PARAM, encodedFieldOverrides));
    }

    @Override
    protected void reportException(String message) {

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setMessage(form_back_confirm_dialog_message)
                .setTitle(form_back_confirm_dialog_title)
                .setCancelable(false)
                .setPositiveButton(yes_button_label,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                goBack();
                            }
                        })
                .setNegativeButton(no_button_label,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                            }
                        })
                .show();
    }

    private void goBack() {
        super.onBackPressed();
    }

    public class GeoWebChromeClient extends WebChromeClient {
        @Override
        public void onGeolocationPermissionsShowPrompt(String origin,
                                                       GeolocationPermissions.Callback callback) {
            // Always grant permission since the app itself requires location
            // permission and the user has therefore already granted it
            callback.invoke(origin, true, false);
        }
    }
}
