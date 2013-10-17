package org.nsdev.apps.linktester;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by neal on 2013-10-15.
 */
public class PortalKey extends InventoryItem {
    private LatLng location;
    private String portalTitle;
    private String portalAddress;
    private String portalImageURL;
    private String portalGuid;
    private int keyCount = 1;

    public PortalKey(LatLng location, String portalTitle, String portalAddress, String portalImageURL, String portalGuid) {

        this.location = location;
        this.portalTitle = portalTitle;
        this.portalAddress = portalAddress;
        this.portalImageURL = portalImageURL;
        this.portalGuid = portalGuid;
    }

    public LatLng getLocation() {
        return location;
    }

    public String getPortalTitle() {
        return portalTitle;
    }

    public String getPortalAddress() {
        return portalAddress;
    }

    public String getPortalGuid() {
        return portalGuid;
    }

    public void setPortalGuid(String portalGuid) {
        this.portalGuid = portalGuid;
    }

    public String getPortalImageURL() {
        return portalImageURL;
    }

    public int getKeyCount() {
        return keyCount;
    }

    public void incrementKeyCount() {
        keyCount += 1;
    }
}
