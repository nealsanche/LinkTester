package org.nsdev.apps.linktester.helpers;

import com.github.davidmoten.geo.Coverage;
import com.github.davidmoten.geo.GeoHash;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;

/**
 * Created by neal on 12/18/2013.
 */
public class GeohashHelper {

    /**
     * This method finds a single geohash that covers the given radius with a bounding box. It is
     * pretty much guaranteed to give a bigger area than asked for, so be aware of this.
     *
     * @param pos    the position at the center of the bounding box
     * @param radius the radius of the bounding box. This is used to find NW and SE points and build
     *               a bounding box out of that.
     * @return a geohash that can be used as a prefix in a search query.
     */
    public static String getCoveringHash(LatLng pos, double radius) {
        LatLng nw = SphericalUtil.computeOffset(pos, radius, -45);
        LatLng se = SphericalUtil.computeOffset(pos, radius, 135);

        Coverage coverage = GeoHash.coverBoundingBoxMaxHashes(nw.latitude, nw.longitude, se.latitude, se.longitude, 1);

        return coverage.getHashes().toArray(new String[1])[0];
    }

}
