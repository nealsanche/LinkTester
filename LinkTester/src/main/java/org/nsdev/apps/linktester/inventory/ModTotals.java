package org.nsdev.apps.linktester.inventory;

import java.util.HashMap;

/**
 * Created by neal on 2013-10-20.
 */
public class ModTotals extends InventoryItem {

    // Rarity -> Display Name -> Count
    private HashMap<String, HashMap<String, Integer>> mods = new HashMap<String, HashMap<String, Integer>>();

    public HashMap<String, HashMap<String, Integer>> getMods() {
        return mods;
    }
}
