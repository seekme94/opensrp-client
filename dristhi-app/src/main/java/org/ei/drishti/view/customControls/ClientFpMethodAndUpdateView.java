package org.ei.drishti.view.customControls;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.apache.commons.lang3.StringUtils;
import org.ei.drishti.R;
import org.ei.drishti.domain.FPMethod;
import org.ei.drishti.view.contract.FPSmartRegisterClient;

public class ClientFpMethodAndUpdateView extends LinearLayout {
    private TextView fpMethodView;
    private TextView fpMethodDateView;
    private TextView fpMethodQuantityLabelView;
    private TextView fpMethodQuantityView;
    private TextView iudPlaceView;
    private TextView iudPersonView;
    private TextView fpMethodUpdateView;

    @SuppressWarnings("UnusedDeclaration")
    public ClientFpMethodAndUpdateView(Context context) {
        this(context, null, 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public ClientFpMethodAndUpdateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ClientFpMethodAndUpdateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void initialize() {
        fpMethodView = (TextView) findViewById(R.id.txt_fp_method);
        fpMethodDateView = (TextView) findViewById(R.id.txt_fp_method_date);
        fpMethodQuantityLabelView = (TextView) findViewById(R.id.txt_fp_method_quantity_label);
        fpMethodQuantityView = (TextView) findViewById(R.id.txt_fp_method_quantity);
        iudPlaceView = (TextView) findViewById(R.id.txt_iud_place);
        iudPersonView = (TextView) findViewById(R.id.txt_iud_person);
        fpMethodUpdateView = (TextView) findViewById(R.id.txt_fp_method_update);
    }

    public void bindData(FPSmartRegisterClient client, int txtColorBlack) {
        FPMethod fpMethod = client.fpMethod();

        refreshAllFPMethodDetailViews(txtColorBlack);

        fpMethodView.setText(fpMethod.displayName());
        fpMethodDateView.setVisibility(View.VISIBLE);
        fpMethodDateView.setText(client.familyPlanningMethodChangeDate());
        fpMethodUpdateView.setText("Update");

        if (fpMethod == FPMethod.NONE) {
            fpMethodView.setTextColor(Color.RED);
            fpMethodDateView.setVisibility(View.GONE);
        } else if (fpMethod == FPMethod.OCP) {
            fpMethodQuantityLabelView.setVisibility(View.VISIBLE);
            fpMethodQuantityView.setVisibility(View.VISIBLE);
            fpMethodQuantityView.setText(client.numberOfOCPDelivered());
        } else if (fpMethod == FPMethod.CONDOM) {
            fpMethodQuantityLabelView.setVisibility(View.VISIBLE);
            fpMethodQuantityView.setVisibility(View.VISIBLE);
            fpMethodQuantityView.setText(client.numberOfCondomsSupplied());
        } else if (fpMethod == FPMethod.CENTCHROMAN) {
            fpMethodQuantityLabelView.setVisibility(View.VISIBLE);
            fpMethodQuantityView.setVisibility(View.VISIBLE);
            fpMethodQuantityView.setText(client.numberOfCentchromanPillsDelivered());
        } else if (fpMethod == FPMethod.IUD) {
            if (StringUtils.isNotBlank(client.iudPerson())) {
                iudPersonView.setVisibility(View.VISIBLE);
                iudPersonView.setText(client.iudPerson());
            }
            if (StringUtils.isNotBlank(client.iudPlace())) {
                iudPlaceView.setVisibility(View.VISIBLE);
                iudPlaceView.setText(client.iudPlace());
            }
        }
    }

    public void refreshAllFPMethodDetailViews(int fpMethodTextColor) {
        fpMethodView.setTextColor(fpMethodTextColor);
        fpMethodDateView.setVisibility(View.GONE);
        fpMethodQuantityLabelView.setVisibility(View.GONE);
        fpMethodQuantityView.setVisibility(View.GONE);
        iudPersonView.setVisibility(View.GONE);
        iudPlaceView.setVisibility(View.GONE);
    }
}
