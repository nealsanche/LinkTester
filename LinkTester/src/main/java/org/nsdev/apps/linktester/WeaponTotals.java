package org.nsdev.apps.linktester;

import java.util.HashMap;

/**
 * Created by neal on 2013-10-20.
 */
public class WeaponTotals extends InventoryItem {
    private HashMap<Integer, Integer> empWeapons = new HashMap<Integer, Integer>();
    private HashMap<String, Integer> flipCards = new HashMap<String, Integer>();

    public HashMap<Integer, Integer> getEmpWeapons() {
        return empWeapons;
    }

    public HashMap<String, Integer> getFlipCards() {
        return flipCards;
    }
}
