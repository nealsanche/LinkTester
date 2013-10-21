package org.nsdev.apps.linktester;

/**
 * Created by neal on 2013-10-20.
 */
public class InventoryTotal extends InventoryItem {
    private final int totalInventoryCount;

    public InventoryTotal(int totalInventoryCount) {
        this.totalInventoryCount = totalInventoryCount;
    }

    public int getTotalInventoryCount() {
        return totalInventoryCount;
    }
}
