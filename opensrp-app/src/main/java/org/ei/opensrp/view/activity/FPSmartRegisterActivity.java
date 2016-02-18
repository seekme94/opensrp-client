package org.ei.opensrp.view.activity;

import org.ei.opensrp.view.controller.FPSmartRegisterController;

public class FPSmartRegisterActivity extends SmartRegisterActivity {

    @Override
    protected void onSmartRegisterInitialization() {
        webView.addJavascriptInterface(new FPSmartRegisterController(), "context");
        webView.loadUrl("file:///android_asset/www/smart_registry/fp_register.html");
    }
}
