package org.nsdev.apps.linktester.model;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.util.ArrayList;
import java.util.List;

class DatabaseHelper extends OrmLiteSqliteOpenHelper {
    private static final String TAG = "LINKTESTER-DB";

    // name of the database file for your application -- change to something appropriate for your app
    private static final String DATABASE_NAME = "LinkTester.sqlite";

    // any time you make changes to your database objects, you may have to increase the database version
    private static final int DATABASE_VERSION = 1;

    // the DAO object we use to access the Favourites table
    private Dao<Portal, Integer> portalDao = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTable(connectionSource, Portal.class);
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "Can't create database", e);
            throw new RuntimeException(e);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            List<String> allSql = new ArrayList<String>();
            switch (oldVersion) {
                /* Some examples of what to do to migrate a schema through a set of versions
                case 1:
                    Log.i(TAG, "Upgrading from database version 1");
                    allSql.add("alter table Hack add column `runningHotSeconds` INTEGER");
                case 2:
                    Log.i(TAG, "Upgrading from database version 2");
                    allSql.add("alter table Hack add column `burnedOut` INTEGER");
                case 3:
                    Log.i(TAG, "Upgrading from database version 3");
                    allSql.add("create table Hack_tmp as select id, latitude, longitude, firstHacked, runningHotSeconds as coolDownSeconds, lastHacked, hackCount, burnedOut from Hack");
                    allSql.add("drop table Hack");
                    allSql.add("alter table Hack_tmp rename to Hack");
                */
            }
            for (String sql : allSql) {
                db.execSQL(sql);
            }
        } catch (SQLException e) {
            Log.e(DatabaseHelper.class.getName(), "exception during onUpgrade", e);
            throw new RuntimeException(e);
        }

    }

    public Dao<Portal, Integer> getPortalDao() {
        if (null == portalDao) {
            try {
                portalDao = getDao(Portal.class);
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
            }
        }
        return portalDao;
    }

}
