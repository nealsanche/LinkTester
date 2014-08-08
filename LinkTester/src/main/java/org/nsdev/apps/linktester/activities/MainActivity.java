package org.nsdev.apps.linktester.activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
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
import android.view.View;
import android.view.Window;
import android.view.animation.AnticipateOvershootInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.SphericalUtil;
import com.twotoasters.clusterkraf.ClusterPoint;
import com.twotoasters.clusterkraf.Clusterkraf;
import com.twotoasters.clusterkraf.InputPoint;
import com.twotoasters.clusterkraf.OnCameraChangeDownstreamListener;
import com.twotoasters.clusterkraf.OnInfoWindowClickDownstreamListener;
import com.twotoasters.clusterkraf.OnMarkerClickDownstreamListener;
import com.twotoasters.clusterkraf.Options;

import org.jenetics.Chromosome;
import org.jenetics.EnumGene;
import org.jenetics.GeneticAlgorithm;
import org.jenetics.Genotype;
import org.jenetics.NumberStatistics;
import org.jenetics.Optimize;
import org.jenetics.PartiallyMatchedCrossover;
import org.jenetics.PermutationChromosome;
import org.jenetics.Phenotype;
import org.jenetics.Statistics;
import org.jenetics.SwapMutator;
import org.jenetics.util.Factory;
import org.jenetics.util.Function;
import org.nsdev.apps.linktester.PortalMarkerOptionsChooser;
import org.nsdev.apps.linktester.R;
import org.nsdev.apps.linktester.model.DatabaseManager;
import org.nsdev.apps.linktester.model.Portal;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {

    private static final String TAG = "LinkTester";
    public static final String ORG_NSDEV_APPS_LINK_TESTER_SELECTED_USER = "org.nsdev.apps.LinkTester.selectedUser";
    public static final String ORG_NSDEV_APPS_LINK_TESTER_MAP_LOCATION = "org.nsdev.apps.LinkTester.mapLocation";

    private GoogleMap map;

    private Stack<LatLng> distanceStack = new Stack<LatLng>();
    private Clusterkraf mClusterkraf;

    private ArrayList<Object> linkMapItems = new ArrayList<Object>();

    private AtomicBoolean dataLoaded = new AtomicBoolean(false);
    private LocationClient client;

    private DatabaseManager mDatabaseManager;

    private Button stopButton;
    private TextView distance;

    private boolean mStopRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stopButton = (Button) findViewById(R.id.button);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStopRequested = true;
            }
        });
        distance = (TextView) findViewById(R.id.distance);

        mDatabaseManager = new DatabaseManager(this);

        // Get a handle to the Map Fragment
        map = ((MapFragment) getFragmentManager()
                .findFragmentById(R.id.map)).getMap();

        Intent intent = getIntent();

        if (Intent.ACTION_SEND.equals(intent.getAction())) {

            String title = (String) intent.getExtras().get(Intent.EXTRA_SUBJECT);
            String intelUrl = (String) intent.getExtras().get(Intent.EXTRA_TEXT);

            Log.e("NAS", String.format("%s %s", title, intelUrl));

            final Uri uri = Uri.parse(intelUrl);
            String ll = uri.getQueryParameter("ll");
            int z = Integer.parseInt(uri.getQueryParameter("z"));
            String[] parts = ll.split(",");
            double lat = Double.parseDouble(parts[0]);
            double lon = Double.parseDouble(parts[1]);

            double latitudeFromIntent = lat;
            double longitudeFromIntent = lon;
            String nameFromIntent = title;

            Log.e("NAS", String.format("%f %f %s", latitudeFromIntent, longitudeFromIntent, nameFromIntent));

            Portal p = new Portal();
            p.setName(nameFromIntent);
            p.setLatitude(latitudeFromIntent);
            p.setLongitude(longitudeFromIntent);
            mDatabaseManager.save(p);

            Toast.makeText(getApplicationContext(), "Saved " + nameFromIntent, Toast.LENGTH_SHORT).show();
            finish();

        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Log.e("NAS", intent.toString());
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

                    ActionMode actionMode = startActionMode(new ActionMode.Callback() {
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
                            switch (item.getItemId()) {
                                case R.id.ic_action_link:
                                    linkToOrFromMarker(marker, clusterPoint);
                                    mode.finish();
                                    return true;
                                case R.id.ic_portal_delete:
                                    Portal p = (Portal) clusterPoint.getPointAtOffset(0).getTag();
                                    mDatabaseManager.delete(p);
                                    reloadPortals();
                                    mode.finish();
                                    return true;
                                case R.id.ic_action_shortest_path:
                                    calculateShortestPath();
                                    mode.finish();
                                    return true;
                                case R.id.ic_action_maps:
                                    showInMaps(marker.getPosition(), marker.getTitle());
                            }
                            return false;
                        }

                        @Override
                        public void onDestroyActionMode(ActionMode mode) {

                        }
                    });
                    if (actionMode != null && marker.getTitle() != null) {
                        actionMode.setTitle(marker.getTitle());
                    }
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
                    reloadPortals();
                }

                SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_MAP_LOCATION, Context.MODE_PRIVATE);

                prefs.edit().putLong("latitude", (long) (cameraPosition.target.latitude * 1E6))
                        .putLong("longitude", (long) (cameraPosition.target.longitude * 1E6))
                        .putFloat("zoom", cameraPosition.zoom)
                        .apply();

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

            createLink(first, second);
        }
    }

    private void createLink(LatLng first, LatLng second) {
        float[] results = new float[2];
        Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, results);

        String distance = String.format("Distance: %.2f km", results[0] / 1000.0);

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

        if (item.getItemId() == R.id.action_show_location) {
            if (map != null)
                map.setMyLocationEnabled(!map.isMyLocationEnabled());
        } else if (item.getItemId() == R.id.action_clear_links) {
            clearLinks();
        } else if (item.getItemId() == R.id.action_clear_database) {

            List<Portal> portals = mDatabaseManager.getAll();

            for (Portal portal : portals) {
                mDatabaseManager.delete(portal);
                mClusterkraf.replace(new ArrayList<InputPoint>());
            }
        }


        return super.onOptionsItemSelected(item);
    }

    private void clearLinks() {
        for (Object o : linkMapItems) {

            if (o instanceof Polyline) {
                ((Polyline) o).remove();
            } else if (o instanceof Marker) {
                ((Marker) o).remove();
            }
        }

        linkMapItems.clear();
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

    private void saveAccount(String accountName, String accountType) {
        // Remember the accountName and accountType for later
        SharedPreferences prefs = getSharedPreferences(ORG_NSDEV_APPS_LINK_TESTER_SELECTED_USER, Context.MODE_PRIVATE);

        prefs.edit()
                .putString("accountName", accountName)
                .putString("accountType", accountType)
                .apply();
    }

    private void calculateShortestPath() {

        final ArrayList<InputPoint> pointsToAdd = new ArrayList<InputPoint>();

        for (Portal p : mDatabaseManager.getAll()) {
            final LatLng latLng = new LatLng(p.getLatitude(), p.getLongitude());
            pointsToAdd.add(new InputPoint(latLng, p));
        }

        distance.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.VISIBLE);
        mStopRequested = false;

        AsyncTask<Void, Void, Phenotype<EnumGene<Integer>, Double>> task = new AsyncTask<Void, Void, Phenotype<EnumGene<Integer>, Double>>() {
            @Override
            protected Phenotype<EnumGene<Integer>, Double> doInBackground(Void... params) {

                final Factory<Genotype<EnumGene<Integer>>> gtf = Genotype.of(PermutationChromosome.ofInteger(pointsToAdd.size()));
                Function<? super Genotype<EnumGene<Integer>>, ? extends Double> fitnessFunction = new Function<Genotype<EnumGene<Integer>>, Double>() {
                    @Override
                    public Double apply(Genotype<EnumGene<Integer>> genotype) {

                        final Chromosome<EnumGene<Integer>> path = genotype.getChromosome();

                        double length = 0.0;
                        for (int i = 0, n = path.length(); i < n; ++i) {
                            final int from = path.getGene(i).getAllele();
                            final int to = path.getGene((i + 1) % n).getAllele();

                            LatLng first = pointsToAdd.get(from).getMapPosition();
                            LatLng second = pointsToAdd.get(to).getMapPosition();

                            float[] results = new float[2];
                            Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, results);

                            length += results[0];
                        }

                        return length;
                    }
                };

                setIndeterminate(true);

                final GeneticAlgorithm<EnumGene<Integer>, Double> ga = new GeneticAlgorithm<EnumGene<Integer>, Double>(gtf, fitnessFunction, Optimize.MINIMUM);
                ga.setStatisticsCalculator(new NumberStatistics.Calculator<EnumGene<Integer>, Double>());
                ga.setPopulationSize(500);
                //noinspection unchecked
                ga.setAlterers(new SwapMutator<EnumGene<Integer>>(0.15),
                        new PartiallyMatchedCrossover<Integer>(0.6));

                ga.setup();
                ga.evolve(new Function<Statistics<EnumGene<Integer>, Double>, Boolean>() {
                    @Override
                    public Boolean apply(Statistics<EnumGene<Integer>, Double> enumGeneDoubleStatistics) {
                        final Double fitness = enumGeneDoubleStatistics.getBestPhenotype().getFitness();
                        Log.e("FIT", "Best fitness: " + fitness);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                distance.setText(String.format("%.2f km", fitness / 1000.0));
                            }
                        });
                        return !(mStopRequested);
                    }
                });

                Phenotype<EnumGene<Integer>, Double> bestPhenotype = ga.getBestPhenotype();
                System.out.println(bestPhenotype);

                return bestPhenotype;
            }

            @Override
            protected void onPostExecute(Phenotype<EnumGene<Integer>, Double> bestPhenotype) {

                distance.setVisibility(View.GONE);
                stopButton.setVisibility(View.GONE);

                setIndeterminate(false);
                clearLinks();

                Chromosome<EnumGene<Integer>> path = bestPhenotype.getGenotype().getChromosome();

                double length = 0.0;
                for (int i = 0, n = path.length(); i < n; ++i) {
                    final int from = path.getGene(i).getAllele();
                    final int to = path.getGene((i + 1) % n).getAllele();

                    LatLng first = pointsToAdd.get(from).getMapPosition();
                    LatLng second = pointsToAdd.get(to).getMapPosition();

                    float[] results = new float[2];
                    Location.distanceBetween(first.latitude, first.longitude, second.latitude, second.longitude, results);

                    length += results[0];

                    createLink(first, second);
                }

                Toast.makeText(MainActivity.this, String.format("Distance: %.2f km", length / 1000), Toast.LENGTH_LONG).show();
            }
        };

        task.execute();
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

    private void showInMaps(LatLng portalPosition, String name) {
        String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?&daddr=%f,%f (%s)", portalPosition.latitude, portalPosition.longitude, name);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            try {
                Intent unrestrictedIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(unrestrictedIntent);
            } catch (ActivityNotFoundException innerEx) {
                Toast.makeText(this, "Please install a maps application", Toast.LENGTH_LONG).show();
            }
        }
    }


}
