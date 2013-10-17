package org.nsdev.apps.linktester;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by neal on 2013-10-15.
 */
public class IngressInventoryParser {

    public static List<InventoryItem> parse(String json) throws JSONException {

        JSONObject result = new JSONObject(json);
        JSONArray inventory = result.getJSONObject("gameBasket").getJSONArray("inventory");

        ArrayList<InventoryItem> items = new ArrayList<InventoryItem>(inventory.length());

        HashMap<String, PortalKey> keyMap = new HashMap<String, PortalKey>();

        for (int i = 0; i < inventory.length(); i++)
        {
            JSONArray itemArray = inventory.getJSONArray(i);

            String itemId = itemArray.getString(0);
            int timeStamp = itemArray.getInt(1);
            JSONObject item = itemArray.getJSONObject(2);

            if (item.has("resource")) {
                JSONObject resource = item.getJSONObject("resource");
                if ("PORTAL_LINK_KEY".equals(resource.getString("resourceType"))) {
                    JSONObject portalCoupler = item.getJSONObject("portalCoupler");

                    String portalGuid = portalCoupler.getString("portalGuid");
                    String portalLocation = portalCoupler.getString("portalLocation");
                    String portalImageURL = portalCoupler.getString("portalImageUrl");
                    String portalTitle = portalCoupler.getString("portalTitle");
                    String portalAddress = portalCoupler.getString("portalAddress");

                    LatLng location = parsePortalLocation(portalLocation);

                    if (keyMap.containsKey(portalGuid)) {

                        keyMap.get(portalGuid).incrementKeyCount();

                    } else {

                        PortalKey portalKey = new PortalKey(location, portalTitle, portalAddress, portalImageURL, portalGuid);
                        items.add(portalKey);
                        keyMap.put(portalGuid, portalKey);

                    }
                }
            }
        }



        return items;
    }

    private static LatLng parsePortalLocation(String portalLocation) {
        String[] parts = portalLocation.split(",");
        long latE6 = Long.parseLong(parts[0],16);
        long lonE6 = Long.parseLong(parts[1],16);

        if (latE6 >= 2147483648L) latE6 -= 4294967296L;
        if (lonE6 >= 2147483648L) lonE6 -= 4294967296L;

        return new LatLng((double)latE6 / 1E6, (double)lonE6 / 1E6);
    }
}
