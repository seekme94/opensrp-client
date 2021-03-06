package org.ei.opensrp.vaccinator.child;

import org.ei.opensrp.commonregistry.CommonPersonObjectClient;
import org.ei.opensrp.view.contract.SmartRegisterClient;
import org.ei.opensrp.view.contract.SmartRegisterClients;
import org.ei.opensrp.view.dialog.SortOption;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Created by Ahmed on 15-Oct-15.
 */
public class ChildDateSort implements SortOption {


    String field;
    ByColumnAndByDetails byColumnAndByDetails;

    public enum ByColumnAndByDetails {
        byColumn, byDetails;
    }

    public ChildDateSort(ByColumnAndByDetails byColumnAndByDetails, String field) {
        this.byColumnAndByDetails = byColumnAndByDetails;
        this.field = field;
    }

    @Override
    public String name() {
        return "Due Status";
    }

    @Override
    public SmartRegisterClients sort(SmartRegisterClients allClients) {
        Collections.sort(allClients, commoncomparator);
        return allClients;
    }

    Comparator<SmartRegisterClient> commoncomparator = new Comparator<SmartRegisterClient>() {
        @Override
        public int compare(SmartRegisterClient oneClient, SmartRegisterClient anotherClient2) {
            CommonPersonObjectClient commonPersonObjectClient = (CommonPersonObjectClient) oneClient;
            CommonPersonObjectClient commonPersonObjectClient2 = (CommonPersonObjectClient) anotherClient2;
            switch (byColumnAndByDetails) {
                case byColumn:
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date1 = dateFormat.parse(commonPersonObjectClient.getColumnmaps().get(field));
                        Date date2 = dateFormat.parse(commonPersonObjectClient2.getColumnmaps().get(field));

                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        break;
                    }


                case byDetails:
                    try {
                        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                        Date date1 = dateFormat.parse(commonPersonObjectClient.getDetails().get(field));
                        Date date2 = dateFormat.parse(commonPersonObjectClient2.getDetails().get(field));
                        return date1.compareTo(date2);
                    } catch (Exception e) {
                        break;
                    }

            }
            return 0;
        }
    };
}
