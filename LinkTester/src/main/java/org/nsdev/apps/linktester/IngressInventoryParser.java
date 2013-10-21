package org.nsdev.apps.linktester;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

        items.add(new InventoryTotal(inventory.length()));

        ModTotals mods = new ModTotals();
        items.add(mods);

        PowerCubeTotals powercubes = new PowerCubeTotals();
        items.add(powercubes);

        WeaponTotals weapons = new WeaponTotals();
        items.add(weapons);

        ResonatorTotals resonators = new ResonatorTotals();
        items.add(resonators);

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
                } else if ("FLIP_CARD".equals(resource.getString("resourceType"))) {
                    if (item.has("flipCard")) {
                        JSONObject flipCard = item.getJSONObject("flipCard");
                        String flipCardType = flipCard.getString("flipCardType");
                        if (!weapons.getFlipCards().containsKey(flipCardType)) {
                            weapons.getFlipCards().put(flipCardType, 1);
                        } else {
                            Integer value = weapons.getFlipCards().get(flipCardType);
                            weapons.getFlipCards().put(flipCardType, value.intValue() + 1);
                        }
                    }
                }
            } else if (item.has("resourceWithLevels")) {
                JSONObject resourceWithLevels = item.getJSONObject("resourceWithLevels");
                String resourceType = resourceWithLevels.getString("resourceType");
                Integer level = resourceWithLevels.getInt("level");

                if (resourceType.equals("EMP_BURSTER")) {
                    if (!weapons.getEmpWeapons().containsKey(level)) {
                        weapons.getEmpWeapons().put(level, 1);
                    } else {
                        Integer value = weapons.getEmpWeapons().get(level);
                        weapons.getEmpWeapons().put(level, value.intValue() + 1);
                    }
                } else if (resourceType.equals("EMITTER_A")) {
                    if (!resonators.getResonators().containsKey(level)) {
                        resonators.getResonators().put(level, 1);
                    } else {
                        Integer value = resonators.getResonators().get(level);
                        resonators.getResonators().put(level, value.intValue() + 1);
                    }
                } else if (resourceType.equals("POWER_CUBE")) {
                    if (!powercubes.getPowerCubes().containsKey(level)) {
                        powercubes.getPowerCubes().put(level, 1);

                    } else {
                        Integer value = powercubes.getPowerCubes().get(level);
                        powercubes.getPowerCubes().put(level, value.intValue() + 1);
                    }
                }
            } else if (item.has("modResource")) {
                JSONObject modResource = item.getJSONObject("modResource");
                String rarity = modResource.getString("rarity");
                String displayName = modResource.getString("displayName");
                if (!mods.getMods().containsKey(rarity)) {
                    final HashMap<String, Integer> modCountMap = new HashMap<String, Integer>();
                    modCountMap.put(displayName, 1);
                    mods.getMods().put(rarity, modCountMap);
                } else {
                    final HashMap<String, Integer> modCountMap = mods.getMods().get(rarity);

                    if (modCountMap.containsKey(displayName)) {
                        // Increment
                        Integer value = modCountMap.get(displayName);
                        modCountMap.put(displayName, value.intValue() + 1);
                    } else {
                        modCountMap.put(displayName, 1);
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
