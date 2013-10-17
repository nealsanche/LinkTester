package org.nsdev.apps.linktester;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;
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
import com.twotoasters.clusterkraf.Options;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;

import java.io.File;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.GsonConverter;

public class MainActivity extends Activity {

    private static final String HTTPS_SERVER_ADDRESS = "https://m-dot-betaspike.appspot.com";
    private static final String TAG = "LinkTester";
    private OkHttpClient okHttpClient;
    private int SOME_REQUEST_CODE = 999;
    private CookieManager cookieManager;
    private String mXXsrfToken;
    private GoogleMap map;

    private HashMap<Marker, PortalKey> markerKeys = new HashMap<Marker, PortalKey>();

    private Stack<LatLng> distanceStack = new Stack<LatLng>();
    private Clusterkraf mClusterkraf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get a handle to the Map Fragment
        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        LatLng sydney = new LatLng(-33.867, 151.206);

        map.setMyLocationEnabled(false);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(sydney, 13));

        map.addMarker(new MarkerOptions()
                .title("Sydney")
                .snippet("The most populous city in Australia.")
                .position(sydney));

        Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
                false, null, null, null, null);
        startActivityForResult(intent, SOME_REQUEST_CODE);

        com.twotoasters.clusterkraf.Options options = new com.twotoasters.clusterkraf.Options();

        options.setTransitionDuration(500);
        options.setPixelDistanceToJoinCluster(30);
        options.setZoomToBoundsPadding(75);
        options.setZoomToBoundsAnimationDuration(750);
        options.setTransitionInterpolator(new AnticipateOvershootInterpolator());
        options.setMarkerOptionsChooser(new PortalMarkerOptionsChooser(this));
        options.setSinglePointClickBehavior(Options.SinglePointClickBehavior.SHOW_INFO_WINDOW_NO_CENTER);
        options.setOnInfoWindowClickDownstreamListener(new OnInfoWindowClickDownstreamListener() {
            @Override
            public boolean onInfoWindowClick(Marker marker, ClusterPoint clusterPoint) {

                if (clusterPoint != null)
                    distanceStack.push(marker.getPosition());

                if (distanceStack.size() == 2) {
                    LatLng first = distanceStack.pop();
                    LatLng second = distanceStack.pop();

                    float[] results = new float[2];
                    Location.distanceBetween(first.latitude,first.longitude,second.latitude,second.longitude, results);

                    String distance = String.format("Distance: %.2f km", results[0] / 1000.0);

                    Toast.makeText(getApplicationContext(), distance, Toast.LENGTH_LONG).show();

                    PolylineOptions options = new PolylineOptions();
                    options.add(first);
                    options.add(second);
                    options.color(Color.BLUE);
                    options.geodesic(true);

                    map.addPolyline(options);

                    LatLng center = SphericalUtil.interpolate(first, second, 0.5);

                    MarkerOptions midPointMarker = new MarkerOptions();
                    midPointMarker.position(center);
                    midPointMarker.title(distance);
                    midPointMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_link_center));
                    midPointMarker.anchor(0.5f,0.5f);

                    map.addMarker(midPointMarker);
                }

                return false;
            }
        });
        options.setOnCameraChangeDownstreamListener(new OnCameraChangeDownstreamListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
            }
        });
        mClusterkraf = new Clusterkraf(map, options, null);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
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
            Log.e(TAG, "Unable to create http cache.", e);
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
                Log.e(TAG, "Intent = " + extras);

                AccountManager accountManager = AccountManager.get(getApplicationContext());
                String accountName = extras.getString(AccountManager.KEY_ACCOUNT_NAME);
                String accountType = extras.getString(AccountManager.KEY_ACCOUNT_TYPE);
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
                        Intent intent = (Intent)bundle.get(AccountManager.KEY_INTENT);
                        if(intent != null) {
                            // User input required
                            startActivity(intent);
                        } else {
                            onGetAuthToken(bundle);
                        }
                    }
                }, null);
            }
        }
    }

    private void onGetAuthToken(Bundle bundle) {
        Log.e(TAG, "onGetAuthToken" + bundle);

        String authtoken = bundle.getString(AccountManager.KEY_AUTHTOKEN);

        final InventoryServiceAsync service = getService();
        service.authenticat(authtoken, new Callback<Response>() {
            @Override
            public void success(Response response, Response response2) {
                Log.e(TAG, "Success");
            }

            @Override
            public void failure(RetrofitError error) {

                boolean foundSacsid = false;
                if (error.getResponse().getStatus() == 302)
                {
                    // An error is expected here, a 302 redirect
                    for (retrofit.client.Header header : error.getResponse().getHeaders())
                    {
                        if ("set-cookie".equals(header.getName())) {
                            Log.e(TAG, "Header: " + header.getName() + " v= " + header.getValue());
                        }
                    }

                    try {
                        List<HttpCookie> httpCookies = cookieManager.getCookieStore().get(new URI(HTTPS_SERVER_ADDRESS));
                        for (HttpCookie cookie : httpCookies) {
                            Log.e(TAG, "Cookie: " + cookie.getName() + " v = " + cookie.getValue());
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
                                    Log.e(TAG, "Success!!!!");
                                    try {
                                        String body = IOUtils.toString(r2.getBody().in());

                                        // Look for setRequestHeader('X-XsrfToken', 'xQuLs8jn9YO53oAWPLF6y-4Q_0g:1381730255013')

                                        Pattern p = Pattern.compile(".*'X-XsrfToken', '([^']+)'\\)", Pattern.DOTALL);

                                        Matcher m = p.matcher(body);
                                        if (m.find()) {
                                            String token = m.group(1);

                                            Log.e(TAG, "Found X-XsrfToken: " + token);
                                            mXXsrfToken = token;

                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {

                                                   service.getInventory(mXXsrfToken, new Params(), new Callback<Response>() {
                                                        @Override
                                                        public void success(Response inventory, Response response) {
                                                            Log.e(TAG, "Inventory Success!");

                                                            try {
                                                                String json = IOUtils.toString(inventory.getBody().in());

                                                                final List<InventoryItem> keys = IngressInventoryParser.parse(json);

                                                                Log.e(TAG, "keys.size = " + keys.size());

                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {

                                                                        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
                                                                        ArrayList<InputPoint> points = new ArrayList<InputPoint>();

                                                                        for (InventoryItem item : keys) {
                                                                            PortalKey key = (PortalKey)item;

                                                                            LatLng position = new LatLng(key.getLocation().latitude, key.getLocation().longitude);
                                                                            points.add(new InputPoint(position, key));

                                                                            bounds.include(key.getLocation());


                                                                            /*
                                                                            Marker m = map.addMarker(new MarkerOptions()
                                                                                    .title(key.getPortalTitle())
                                                                                    .snippet(key.getPortalAddress())
                                                                                    .anchor(0.5f,0.5f)
                                                                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_portal))
                                                                                    .position(key.getLocation()));

                                                                            markerKeys.put(m, key);
                                                                            */

                                                                        }

                                                                        mClusterkraf.replace(points);

                                                                        LatLngBounds latLngBounds = bounds.build();

                                                                        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(latLngBounds, 100);

                                                                        map.animateCamera(cameraUpdate);


                                                                    }
                                                                });

                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            } catch (JSONException e) {
                                                                e.printStackTrace();
                                                            }

                                                        }

                                                        @Override
                                                        public void failure(RetrofitError error) {
                                                            Log.e(TAG, "Inventory failed!");
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
                                    Log.e(TAG, "Failed: ", error);
                                }
                            });
                        }
                    });

                }
            }
        });

    }
}
