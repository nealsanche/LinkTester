package org.nsdev.apps.linktester.inventory;

import java.util.HashMap;

/**
 * Created by neal on 2013-10-21.
 */
public class PowerCubeTotals extends InventoryItem {
    private HashMap<Integer, Integer> powerCubes = new HashMap<Integer, Integer>();

    public HashMap<Integer, Integer> getPowerCubes() {
        return powerCubes;
    }
}
