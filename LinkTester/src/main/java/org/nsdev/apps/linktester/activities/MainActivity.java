package org.nsdev.apps.linktester.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.maps.android.SphericalUtil;
import com.squareup.okhttp.HttpResponseCache;
import com.squareup.okhttp.OkAuthenticator;
import com.squareup.okhttp.OkHttpClient;
import com.twotoasters.clusterkraf.ClusterPoint;
import com.twotoasters.clusterkraf.Clusterkraf;
import com.twotoasters.clusterkraf.InputPoint;
import com.twotoasters.clusterkraf.OnCameraChangeDownstreamListener;
import com.twotoasters.clusterkraf.OnInfoWindowClickDownstreamListener;
import com.twotoasters.clusterkraf.OnMarkerClickDownstreamListener;
import com.twotoasters.clusterkraf.Options;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.nsdev.apps.linktester.PortalMarkerOptionsChooser;
import org.nsdev.apps.linktester.R;
import org.nsdev.apps.linktester.inventory.IngressInventoryParser;
import org.nsdev.apps.linktester.inventory.InventoryFragment;
import org.nsdev.apps.linktester.inventory.InventoryItem;
import org.nsdev.apps.linktester.inventory.InventoryServiceAsync;
import org.nsdev.apps.linktester.inventory.InventoryTotal;
import org.nsdev.apps.linktester.inventory.ModTotals;
import org.nsdev.apps.linktester.inventory.Params;
import org.nsdev.apps.linktester.inventory.PortalKey;
import org.nsdev.apps.linktester.inventory.PowerCubeTotals;
import org.nsdev.apps.linktester.inventory.ResonatorTotals;
import org.nsdev.apps.linktester.inventory.WeaponTotals;
import org.nsdev.apps.linktester.model.DatabaseManager;
import org.nsdev.apps.linktester.model.Portal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class MainActivity extends Activity implements InventoryFragment.InventoryFragmentProvider {

    private static final String HTTPS_SERVER_ADDRESS = "https://m-dot-betaspike.appspot.com";
    private static final String TAG = "LinkTester";
    public static final String ORG_NSDEV_APPS_LINK_TESTER_SELECTED_USER = "org.nsdev.apps.LinkTester.selectedUser";
    public static final String ORG_NSDEV_APPS_LINK_TESTER_MAP_LOCATION = "org.nsdev.apps.LinkTester.mapLocation";
    private static final int SOME_REQUEST_CODE = 999;
    private static final int USER_INPUT_REQUIRED = 1001;

    private OkHttpClient okHttpClient;
    private CookieManager cookieManager;
    private String mXXsrfToken;
    private GoogleMap map;
    private String authToken;

    private HashMap<Marker, PortalKey> markerKeys = new HashMap<Marker, PortalKey>();

    private Stack<LatLng> distanceStack = new Stack<LatLng>();
    private Clusterkraf mClusterkraf;

    private ArrayList<Object> linkMapItems = new ArrayList<Object>();

    private AtomicBoolean dataLoaded = new AtomicBoolean(false);
    private LocationClient client;

    private WeaponTotals weaponTotals;
    private ResonatorTotals resonatorTotals;
    private ModTotals modTotals;
    private InventoryTotal inventoryTotal;
    private int portalKeyCount;
    private PowerCubeTotals powerCubeTotals;
    private int mZoom;
    private double mLatitudeFromIntent;
    private double mLongitudeFromIntent;
    private String mNameFromIntent;
    private DatabaseManager mDatabaseManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDatabaseManager = new DatabaseManager(this);

        // Get a handle to the Map Fragment
        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            // We're being asked to view a map location

            Uri data = getIntent().getData();
            String q = data.getQueryParameter("q");
            String z = data.getQueryParameter("z");

            mZoom = Integer.parseInt(z);

            Pattern pattern = Pattern.compile("loc:([^,]*),([^ ]*)\\s*\\(([^\\)]*)\\)");

            final Matcher matcher = pattern.matcher(q);
            if (matcher.find()) {
                mLatitudeFromIntent = Double.parseDouble(matcher.group(1));
                mLongitudeFromIntent = Double.parseDouble(matcher.group(2));
                mNameFromIntent = matcher.group(3);

                Log.e("NAS", String.format("%f %f %s", mLatitudeFromIntent, mLongitudeFromIntent, mNameFromIntent));

                Portal p = new Portal();
                p.setName(mNameFromIntent);
                p.setLatitude(mLatitudeFromIntent);
                p.setLongitude(mLongitudeFromIntent);
                mDatabaseManager.save(p);

                Toast.makeText(getApplicationContext(), "Saved " + mNameFromIntent, Toast.LENGTH_SHORT).show();
                finish();
            }

        }

        map.setMyLocationEnabled(false);

        restoreMapPosition();

        com.twotoasters.clusterkraf.Options options = new com.twotoasters.clusterkraf.Options();

        options.setTransitionDuration(500);
        options.setPixelDistanceToJoinCluster(30);
        options.setZoomToBoundsPadding(75);
        options.setZoomToBoundsAnimationDuration(750);
        options.setTransitionInterpolator(new AnticipateOvershootInterpolator());
        options.setMarkerOptionsChooser(new PortalMarkerOptionsChooser(this));
        options.setSinglePointClickBehavior(Options.SinglePointClickBehavior.SHOW_INFO_WINDOW_NO_CENTER);
        options.setOnMarkerClickDownstreamListener(new OnMarkerClickDownstreamListener() {
            @Override
            public boolean onMarkerClick(final Marker marker, final ClusterPoint clusterPoint) {

                if (clusterPoint != null && clusterPoint.size() == 1) {

                    startActionMode(new ActionMode.Callback() {
                        @Override
                        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                            // Inflate a menu resource providing context menu items
                            MenuInflater inflater = actionMode.getMenuInflater();
                            inflater.inflate(R.menu.portal_actions, menu);
                            return true;
                        }

                        @Override
                        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                            return false;
                        }

                        @Override
                        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                            if (item.getItemId() == R.id.ic_action_link) {
                                linkToOrFromMarker(marker, clusterPoint);
                                mode.finish();
                                return true;
                            } else if (item.getItemId() == R.id.ic_portal_delete) {
                                Portal p = (Portal) clusterPoint.getPointAtOffset(0).getTag();
                                mDatabaseManager.delete(p);
                                reloadPortals();
                                mode.finish();
                                return true;
                            }
                            return false;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {

                        }
                    });
                }

                return false;
            }
        });
        options.setOnInfoWindowClickDownstreamListener(new OnInfoWindowClickDownstreamListener() {
            @Override
            public boolean onInfoWindowClick(Marker marker, ClusterPoint clusterPoint) {

                linkToOrFromMarker(marker, clusterPoint);

                return false;
            }
        });

        options.setOnCameraChangeDownstreamListener(new OnCameraChangeDownstreamListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (!dataLoaded.getAndSet(true)) {

                    /*
                    if (isAccountSaved()) {
                        // Load cached data, then query for more current data
                        startLoadingCachedInventoryThenRefresh();
                    } else {
                        // Prompt for the account
                        Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
                                false, null, null, null, null);
                        startActivityForResult(intent, SOME_REQUEST_CODE);
                    }
                    */

                    reloadPortals();
                }

                SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_MAP_LOCATION, Context.MODE_PRIVATE);

                prefs.edit().putLong("latitude", (long) (cameraPosition.target.latitude * 1E6))
                        .putLong("longitude", (long) (cameraPosition.target.longitude * 1E6))
                        .putFloat("zoom", cameraPosition.zoom)
                        .commit();

            }
        });

        mClusterkraf = new Clusterkraf(map, options, null);


    }

    private void linkToOrFromMarker(Marker marker, ClusterPoint clusterPoint) {
        if (clusterPoint != null)
            distanceStack.push(marker.getPosition());

        if (distanceStack.size() == 2) {
            LatLng first = distanceStack.pop();
            LatLng second = distanceStack.pop();

            float[] results = new float[2];
            Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, results);

            String distance = String.format("Distance: %.2f km", results[0] / 1000.0);

            Toast.makeText(getApplicationContext(), distance, Toast.LENGTH_LONG).show();

            PolylineOptions options = new PolylineOptions();
            options.add(first);
            options.add(second);
            options.color(Color.BLUE);
            options.geodesic(true);

            linkMapItems.add(map.addPolyline(options));

            LatLng center = SphericalUtil.interpolate(first, second, 0.5);

            MarkerOptions midPointMarker = new MarkerOptions();
            midPointMarker.position(center);
            midPointMarker.title(distance);
            midPointMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_link_center));
            midPointMarker.anchor(0.5f, 0.5f);

            linkMapItems.add(map.addMarker(midPointMarker));
        }
    }

    private void startLoadingCachedInventoryThenRefresh() {
        // Remember the accountName and accountType for later
        SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_SELECTED_USER, Context.MODE_PRIVATE);

        String accountName = prefs.getString("accountName", null);
        String accountType = prefs.getString("accountType", null);

        if (accountName != null && accountType != null) {
            File cachedInventory = getCachedInventoryFile(accountName, accountType);

            try {
                String json = IOUtils.toString(new FileInputStream(cachedInventory), "utf-8");

                showInventory(json);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        /*
        if (item.getItemId() == R.id.action_signout) {

            SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_SELECTED_USER, Context.MODE_PRIVATE);
            if (prefs.contains("accountName") && prefs.contains("accountType")) {
                String accountName = prefs.getString("accountName", null);
                String accountType = prefs.getString("accountType", null);

                getCachedInventoryFile(accountName, accountType).delete();

                prefs.edit()
                        .remove("accountName")
                        .remove("accountType")
                        .commit();

                AccountManager accountManager = AccountManager.get(getApplicationContext());

                if (authToken != null)
                    accountManager.invalidateAuthToken(accountType, authToken);

                finish();
            }

        } else if (item.getItemId() == R.id.action_refresh) {

            refreshInventory();

        } else
        */
        if (item.getItemId() == R.id.action_show_location) {
            if (map != null)
                map.setMyLocationEnabled(!map.isMyLocationEnabled());
        } else if (item.getItemId() == R.id.action_clear_links) {

            for (Object o : linkMapItems) {

                if (o instanceof Polyline) {
                    ((Polyline) o).remove();
                } else if (o instanceof Marker) {
                    ((Marker) o).remove();
                }
            }

            linkMapItems.clear();
        } else if (item.getItemId() == R.id.action_clear_database) {

            List<Portal> portals = mDatabaseManager.getAll();

            for (Portal portal : portals) {
                mDatabaseManager.delete(portal);
                mClusterkraf.replace(new ArrayList<InputPoint>());
            }
        }


        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        reloadPortals();
    }

    private void reloadPortals() {

        AsyncTask<Void, Void, ArrayList<InputPoint>> task = new AsyncTask<Void, Void, ArrayList<InputPoint>>() {
            @Override
            protected ArrayList<InputPoint> doInBackground(Void... params) {
                ArrayList<InputPoint> pointsToAdd = new ArrayList<InputPoint>();

                for (Portal p : mDatabaseManager.getAll()) {
                    final LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
                    pointsToAdd.add(new InputPoint(latLng, p));
                }
                return pointsToAdd;
            }

            @Override
            protected void onPostExecute(ArrayList<InputPoint> inputPoints) {
                super.onPostExecute(inputPoints);
                mClusterkraf.replace(inputPoints);
            }
        };

        task.execute();
    }

    public InventoryServiceAsync getService() {

        cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(cookieManager);

        // Turn on caching
        okHttpClient = new OkHttpClient();
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, null, null);
        } catch (GeneralSecurityException e) {
            throw new AssertionError(); // The system has no TLS. Just give up.
        }
        okHttpClient.setCookieHandler(cookieManager);
        okHttpClient.setFollowProtocolRedirects(false);
        okHttpClient.setSslSocketFactory(sslContext.getSocketFactory());
        okHttpClient.setAuthenticator(new OkAuthenticator() {
            @Override
            public Credential authenticate(Proxy proxy, URL url, List<Challenge> challenges) throws IOException {
                return null;
            }

            @Override
            public Credential authenticateProxy(Proxy proxy, URL url, List<Challenge> challenges) throws IOException {
                return null;
            }
        });
        File cacheDir = new File(getCacheDir().getPath(), "http_cache");
        HttpResponseCache cache = null;
        try {
            cache = new HttpResponseCache(cacheDir, 4 * 1024 * 1024); // max 4MB downloads stored in cache
            okHttpClient.setResponseCache(cache);
        } catch (IOException e) {
            Log.v(TAG, "Unable to create http cache.", e);
        }

        Executor executor = Executors.newCachedThreadPool();

        RestAdapter adapter = new RestAdapter.Builder()
                .setExecutors(executor, executor)
                .setConverter(new GsonConverter(new Gson()))
                .setClient(new OkClient(okHttpClient))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setServer(HTTPS_SERVER_ADDRESS).build();

        return adapter.create(InventoryServiceAsync.class);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SOME_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                Log.v(TAG, "Intent = " + extras);

                String accountName = extras.getString(AccountManager.KEY_ACCOUNT_NAME, null);
                String accountType = extras.getString(AccountManager.KEY_ACCOUNT_TYPE, null);

                if (accountName != null && accountType != null) {
                    saveAccount(accountName, accountType);
                    getInventoryForAccount(accountName, accountType);
                }
            }
        } else if (requestCode == USER_INPUT_REQUIRED) {
            if (resultCode == RESULT_OK)
                refreshInventory();
        }
    }

    private void restoreMapPosition() {
        // Remember the accountName and accountType for later
        SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_MAP_LOCATION, Context.MODE_PRIVATE);

        if (prefs.contains("latitude") && prefs.contains("longitude")) {
            double latitude = (double) prefs.getLong("latitude", 0) / 1E6;
            double longitude = (double) prefs.getLong("longitude", 0) / 1E6;
            float zoom = prefs.getFloat("zoom", 14);

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), zoom));
        } else {
            // Move the map to the user's current coarse location
            client = new LocationClient(this, new GooglePlayServicesClient.ConnectionCallbacks() {
                @Override
                public void onConnected(Bundle bundle) {
                    Location location = client.getLastLocation();
                    if (location != null)
                        map.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(), location.getLongitude())));
                }

                @Override
                public void onDisconnected() {

                }
            }, new GooglePlayServicesClient.OnConnectionFailedListener() {
                @Override
                public void onConnectionFailed(ConnectionResult connectionResult) {

                }
            }
            );
        }
    }

    private boolean isAccountSaved() {
        // Remember the accountName and accountType for later
        SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_SELECTED_USER, Context.MODE_PRIVATE);

        return (prefs.contains("accountName") && prefs.contains("accountType"));
    }

    private void saveAccount(String accountName, String accountType) {
        // Remember the accountName and accountType for later
        SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_SELECTED_USER, Context.MODE_PRIVATE);

        prefs.edit()
                .putString("accountName", accountName)
                .putString("accountType", accountType)
                .commit();
    }

    private void refreshInventory() {
        SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_SELECTED_USER, Context.MODE_PRIVATE);

        if (prefs.contains("accountName") && prefs.contains("accountType")) {
            String accountName = prefs.getString("accountName", null);
            String accountType = prefs.getString("accountType", null);

            getInventoryForAccount(accountName, accountType);
        }

    }


    private void getInventoryForAccount(final String accountName, final String accountType) {

        AccountManager accountManager = AccountManager.get(getApplicationContext());
        Account account = new Account(accountName, accountType);
        accountManager.getAuthToken(account, "ah", false, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                Bundle bundle = null;
                try {
                    bundle = bundleAccountManagerFuture.getResult();
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }
                Intent intent = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (intent != null) {
                    // User input required
                    startActivityForResult(intent, USER_INPUT_REQUIRED);
                } else {
                    setIndeterminate(true);
                    onGetAuthToken(accountName, accountType, bundle);
                }
            }
        }, null);
    }

    private void onGetAuthToken(final String accountName, final String accountType, Bundle bundle) {
        Log.v(TAG, "onGetAuthToken" + bundle);

        authToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);

        final InventoryServiceAsync service = getService();
        service.authenticat(authToken, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.v(TAG, "Success");
            }

            @Override
            public void failure(RetrofitError error) {

                boolean foundSacsid = false;
                if (error.getResponse().getStatus() == 302) {
                    // An error is expected here, a 302 redirect
                    for (retrofit.client.Header header : error.getResponse().getHeaders()) {
                        if ("set-cookie".equals(header.getName())) {
                            Log.v(TAG, "Header: " + header.getName() + " v= " + header.getValue());
                        }
                    }

                    try {
                        List<HttpCookie> httpCookies = cookieManager.getCookieStore().get(new URI(HTTPS_SERVER_ADDRESS));
                        for (HttpCookie cookie : httpCookies) {
                            Log.v(TAG, "Cookie: " + cookie.getName() + " v = " + cookie.getValue());
                            if ("SACSID".equals(cookie.getName())) {
                                foundSacsid = true;
                            }
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }

                if (foundSacsid) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String json = "{\"params\":{\"lastQueryTimestamp\":0}}";

                            okHttpClient.setFollowProtocolRedirects(true);

                            service.getInventoryRaw(new Callback<Response>() {
                                @Override
                                public void success(Response r1, Response r2) {
                                    Log.v(TAG, "Success!!!!");
                                    try {
                                        String body = IOUtils.toString(r2.getBody().in());

                                        // Look for setRequestHeader('X-XsrfToken', 'xQuLs8jn9YO53oAWPLF6y-4Q_0g:1381730255013')

                                        Pattern p = Pattern.compile(".*'X-XsrfToken', '([^']+)'\\)", Pattern.DOTALL);

                                        Matcher m = p.matcher(body);
                                        if (m.find()) {
                                            String token = m.group(1);

                                            Log.v(TAG, "Found X-XsrfToken: " + token);
                                            mXXsrfToken = token;

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                    service.getInventory(mXXsrfToken, new Params(), new Callback<Response>() {
                                                        @Override
                                                        public void success(Response inventory, Response response) {
                                                            Log.v(TAG, "Inventory Success!");

                                                            try {
                                                                String json = null;
                                                                if (isGzipEncoded(response)) {
                                                                    json = IOUtils.toString(new GZIPInputStream(inventory.getBody().in()));

                                                                } else {
                                                                    json = IOUtils.toString(inventory.getBody().in());
                                                                }
                                                                // Cache the JSON
                                                                File cachedInventory = getCachedInventoryFile(accountName, accountType);
                                                                IOUtils.write(json, new FileOutputStream(cachedInventory));

                                                                showInventory(json);

                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            } finally {
                                                                setIndeterminate(false);
                                                            }

                                                        }

                                                        @Override
                                                        public void failure(RetrofitError error) {
                                                            Log.v(TAG, "Inventory failed!");
                                                        }
                                                    });
                                                }
                                            });
                                        }
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }

                                @Override
                                public void failure(RetrofitError error) {
                                    Log.v(TAG, "Failed: ", error);
                                    setIndeterminate(false);
                                }
                            });
                        }
                    });

                } else {
                    setIndeterminate(false);
                }
            }
        });

    }

    private boolean isGzipEncoded(Response response) {
        for (retrofit.client.Header header : response.getHeaders()) {
            if (header.getName() != null && "content-encoding".equals(header.getName().toLowerCase())) {
                if (header.getValue() != null && "gzip".equals(header.getValue().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private File getCachedInventoryFile(String accountName, String accountType) {
        return new File(getCacheDir(), accountName + "-" + accountType + "-" + "inventory.json");
    }

    private void showInventory(String json) throws JSONException {
        final List<InventoryItem> inventoryItemList = IngressInventoryParser.parse(json);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                ArrayList<InputPoint> points = new ArrayList<InputPoint>();

                portalKeyCount = 0;

                for (InventoryItem item : inventoryItemList) {
                    if (item instanceof PortalKey) {
                        PortalKey key = (PortalKey) item;

                        portalKeyCount += key.getKeyCount();

                        LatLng position = new LatLng(key.getLocation().latitude, key.getLocation().longitude);
                        points.add(new InputPoint(position, key));

                        bounds.include(key.getLocation());
                    } else if (item instanceof InventoryTotal) {
                        inventoryTotal = (InventoryTotal) item;
                        Toast.makeText(getApplicationContext(), String.format("Inventory Count: %d", inventoryTotal.getTotalInventoryCount()), Toast.LENGTH_LONG).show();
                    } else if (item instanceof WeaponTotals) {
                        weaponTotals = (WeaponTotals) item;
                    } else if (item instanceof ModTotals) {
                        modTotals = (ModTotals) item;
                    } else if (item instanceof ResonatorTotals) {
                        resonatorTotals = (ResonatorTotals) item;
                    } else if (item instanceof PowerCubeTotals) {
                        powerCubeTotals = (PowerCubeTotals) item;
                    }
                }

                mClusterkraf.replace(points);

                LatLngBounds latLngBounds = bounds.build();

                CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, 100);

                map.animateCamera(cameraUpdate);

                InventoryFragment inventoryFragment = (InventoryFragment) getFragmentManager().findFragmentById(R.id.inventory);
                inventoryFragment.updateFragment();

            }
        });
    }

    void setIndeterminate(final boolean isIndeterminate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setProgressBarIndeterminate(isIndeterminate);
                setProgressBarIndeterminateVisibility(isIndeterminate);
            }
        });
    }

    @Override
    public WeaponTotals getWeaponTotals() {
        return weaponTotals;
    }

    @Override
    public InventoryTotal getInventoryTotal() {
        return inventoryTotal;
    }

    @Override
    public ModTotals getModTotals() {
        return modTotals;
    }

    @Override
    public ResonatorTotals getResonatorTotals() {
        return resonatorTotals;
    }

    @Override
    public PowerCubeTotals getPowerCubeTotals() {
        return powerCubeTotals;
    }

    @Override
    public int getPortalKeyCount() {
        return portalKeyCount;
    }
}
