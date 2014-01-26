package org.nsdev.apps.linktester.model;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.stmt.QueryBuilder;

import org.nsdev.apps.linktester.helpers.GeohashHelper;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DatabaseManager {
    private DatabaseHelper helper;

    public DatabaseManager(Context ctx) {
        helper = new DatabaseHelper(ctx);
    }

    private DatabaseHelper getHelper() {
        return helper;
    }

    public List<Portal> getAll() {
        List<Portal> portals = null;
        try {
            portals = getHelper().getPortalDao().queryForAll();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return portals;
    }

    public void save(Portal portal) {
        try {
            getHelper().getPortalDao().createOrUpdate(portal);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(Portal portal) {
        try {
            getHelper().getPortalDao().delete(portal);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Portal> findFavesFor(String name) {
        try {
            return getHelper().getPortalDao().queryForEq(Portal.FIELD_NAME_NAME, name);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Portal> findPortalsNear(final LatLng pos, final double radius) throws SQLException {
        QueryBuilder<Portal, Integer> queryBuilder = getHelper().getPortalDao().queryBuilder();

        String hash = GeohashHelper.getCoveringHash(pos, radius);
        List<Portal> portals = queryBuilder.where().like(Portal.FIELD_NAME_GEOHASH, hash + "%").query();

        // Sort by distance
        Collections.sort(portals, new Comparator<Portal>() {
            @Override
            public int compare(Portal lhs, Portal rhs) {
                double dlhs = lhs.distanceTo(pos);
                double drhs = rhs.distanceTo(pos);
                return Double.valueOf(dlhs).compareTo(drhs);
            }
        });

        // Filter on radius
        ArrayList<Portal> retval = new ArrayList<Portal>();
        for (Portal location : portals) {
            if (location.distanceTo(pos) <= radius) retval.add(location);
        }

        return retval;
    }

}