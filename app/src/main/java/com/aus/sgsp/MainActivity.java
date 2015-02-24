package com.aus.sgsp;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

import retrofit.RestAdapter;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.aus.sgsp.Api.*;

public class MainActivity extends ListActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = MainActivity.class.getSimpleName();

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private double mLat;
    private double mLon;
    // Really should be adjustable from menu button.  7k is pretty big though.
    private String mRange = "7km";

    private RetainedFragment dataFragment;
    private Observable<Page.Event> request;
    private Api.SGService myService;
    private Subscription sub;

    private EventAdapter evtAdapter = new EventAdapter(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RestAdapter adapter = Api.getAdapter();
        myService = adapter.create(Api.SGService.class);

        locationStatusCheck();

        // RetainedFragment holds on to the Observable that emits all Events.
        // It's cached, so after a screen flip, it spits the same stuff right back out
        if(getFragmentManager().findFragmentByTag(RetainedFragment.TAG) == null) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            dataFragment = new RetainedFragment();
            ft.add(dataFragment, RetainedFragment.TAG).commit();
        } else {
            dataFragment = (RetainedFragment) getFragmentManager().findFragmentByTag(RetainedFragment.TAG);
            request = dataFragment.getData();
        }

        setListAdapter(evtAdapter);
        getListView().setOnItemClickListener(clickListener);

    }



    // These two recurse until there's one big Observable with every event.
    private static Observable<Page.Event> getAllEvents(SGService service, double lat, double lon,
                                                      String range) {
        return service.getFirstPage(lat, lon, range)
                .flatMap(firstPage -> getNextPages(service, firstPage, lat, lon, range).startWith(firstPage))
                .map(page -> page.events)
                .flatMap(Observable::from);
    }


    private static Observable<Page> getNextPages(SGService service, Page page, double lat,
                                                 double lon, String range) {
        if(page.meta.nextPage() == -1) {
            return Observable.empty();
        }

        return service.getPage(lat, lon, range, page.meta.nextPage())
                .flatMap(nextPage -> Observable.concat(Observable.just(nextPage),
                        getNextPages(service, nextPage, lat, lon, range)));

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(sub != null) {
            sub.unsubscribe();
        }
        dataFragment.setData(request);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupRequestAndSub() {
        if(request == null) {
            request = getAllEvents(myService, mLat, mLon, mRange)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .cache();
        }

        sub = request.subscribe(new Subscriber<Page.Event>() {
            @Override
            public void onCompleted() {
                evtAdapter.localTimeSort();
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "Subscription FAILED", e);
            }

            @Override
            public void onNext(Page.Event event) {
                evtAdapter.addEvent(event);
            }
        });
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {
            mLat = mLastLocation.getLatitude();
            mLon = mLastLocation.getLongitude();
            Log.d("CONNECTED", "Lat:  " + mLat + "  Lon:  "+mLon);
            setupRequestAndSub();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection with Location Services FAILED :: Error code : " +connectionResult.getErrorCode());
    }

    public static class RetainedFragment extends Fragment {

        public static final String TAG = "data";

        public Observable<Page.Event> request;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        public void setData(Observable<Page.Event> savedReq) {
            request = savedReq;
        }

        public Observable<Page.Event> getData() {
            return request;
        }

    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    public void locationStatusCheck()
    {
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                && !manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            // Passive provider is still there, but meh.
            buildAlertMessageNoLocation();
        } else {
            buildGoogleApiClient();
            mGoogleApiClient.connect();
        }

    }
    private void buildAlertMessageNoLocation() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS and network location are disabled.  This app requires them to retrive data." +
                "Would you like to turn them on?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog,  final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    private AdapterView.OnItemClickListener clickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            long eventId = ((Page.Event) parent.getAdapter().getItem(position)).id;
            String url = "seatgeek://events/" + eventId;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));

            PackageManager packageManager = getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(i, 0);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                startActivity(i);
            } else {
                Toast.makeText(getParent(), "SeatGeek is not installed!", Toast.LENGTH_SHORT).show();
            }
        }
    };

}
