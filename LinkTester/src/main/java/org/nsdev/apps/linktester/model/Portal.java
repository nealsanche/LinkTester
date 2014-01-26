package org.nsdev.apps.linktester.model;

import com.github.davidmoten.geo.GeoHash;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;
import com.google.maps.android.SphericalUtil;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Created by neal on 1/25/2014.
 */
@DatabaseTable(tableName = "Portal")
public class Portal {

    @DatabaseField(generatedId = true)
    private int id;

    public static final String FIELD_NAME_NAME = "name";
    @DatabaseField(columnName = FIELD_NAME_NAME)
    @SerializedName("Name")
    private String name;

    public static final String FIELD_NAME_LONGITUDE = "longitude";
    @DatabaseField(columnName = FIELD_NAME_LONGITUDE)
    @SerializedName("Lng")
    private double longitude;

    public static final String FIELD_NAME_LATITUDE = "latitude";
    @DatabaseField(columnName = FIELD_NAME_LATITUDE)
    @SerializedName("Lat")
    private double latitude;

    public static final String FIELD_NAME_GEOHASH = "geohash";
    @DatabaseField(columnName = FIELD_NAME_GEOHASH, index = true)
    @SerializedName("Geohash")
    private String geohash;

    public Portal() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    /**
     * Calculates this retail location's geohash and sets the geohash property to it.
     */
    public void calculateGeohash() {
        if (geohash == null)
            geohash = GeoHash.encodeHash(getLatitude(), getLongitude());
    }

    public double distanceTo(LatLng pos) {
        return SphericalUtil.computeDistanceBetween(pos, new LatLng(getLatitude(), getLongitude()));
    }
}
