package ca.awesome.travis.savemydrone.savemydrone;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.Arrays;
import java.util.List;

import ca.awesome.travis.savemydrone.savemydrone.clouddata.Airport;
import ca.awesome.travis.savemydrone.savemydrone.clouddata.Airports;
import ca.awesome.travis.savemydrone.savemydrone.clouddata.LngLatBox;
import ca.awesome.travis.savemydrone.savemydrone.clouddata.SaveMyDroneApi;
import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, Callback<List<Airport>> {

    private LocationManager locationManager;
    private LatLng currentLngLat;
    private GoogleMap mMap;
    private static final int circleRadius = 1000000;
    private static final int KILOMETRE = 1000;
    public SharedPreferences sharedPreferences;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        sharedPreferences = new SharedPreferences();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

//        initializeLocationManager();


//        goToIntroFragment();
//        goToFlightDetailsFragment();

        getWeather();


    }

    private void initializeLocationManager(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
//                currentLngLat = new LatLng(location.getLatitude(), location.getLongitude());
//                addMarker(currentLngLat, "You are Here");
                updateMap();
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    private void goToIntroFragment(){
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        Fragment fragment = null;

        fragment = (Fragment) new IntroScreenFragment().newInstance();
        fragmentTransaction.addToBackStack(IntroScreenFragment.TAG);
        fragmentTransaction.replace(R.id.popup_frame_layout, fragment,
                IntroScreenFragment.TAG);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void goToFlightDetailsFragment(){
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        Fragment fragment = null;

        fragment = (Fragment) new FlightDetailsFragment().newInstance();
        fragmentTransaction.addToBackStack(FlightDetailsFragment.TAG);
        fragmentTransaction.replace(R.id.popup_frame_layout, fragment,
                IntroScreenFragment.TAG);
        fragmentTransaction.commitAllowingStateLoss();
    }


    private void updateMap() {

        if (currentLngLat != null) {
            addCircle(currentLngLat, "", sharedPreferences.getFlightRange() * KILOMETRE, 1);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLngLat));
            zoomToFit();
        }
        //Draw the circle
        //make web call
        //zoom to relevant area

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        updateMap();
//        LatLng sydney = new LatLng(-10, 151);
//        moveToUserGPS();
//        addCircle(sydney, "Sydney", circleRadius, 10);
//        addPolygon(sydney);
//        addMarker(new LatLng(-34, 151), "marker title");
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));


    }


    public void doneButtonPressed(Fragment fragment){
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.remove(fragment);
        fragmentTransaction.commitAllowingStateLoss();
        updateMap();
    }


    public void getWeather(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://savemydrone.herokuapp.com")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // prepare call in Retrofit 2.0
        SaveMyDroneApi saveMyDroneApi = retrofit.create(SaveMyDroneApi.class);


        LngLatBox lngLatBox = new LngLatBox(-34.846389, -38.846389, 150.793333, 144.793333);
        Call<List<Airport>> call = saveMyDroneApi.getAirportWeathers(lngLatBox);

        call.enqueue(this);
    }


    @Override
    public void onResponse(Response<List<Airport>> response, Retrofit retrofit) {

        sharedPreferences.setAirports(response.body());
        List<Airport> airports = sharedPreferences.getAirports();
        drawAirports();
//        Toast.makeText(MapsActivity.this, "Size is: " + airports.toString() , Toast.LENGTH_SHORT).show();

    }


    @Override
    public void onFailure(Throwable t) {
        Toast.makeText(MapsActivity.this, t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
    }


    private void drawAirports(){
        List<Airport> airports = sharedPreferences.getAirports();

        for (Airport airport: airports){
            LatLng latLng = new LatLng(airport.getLatitude(), airport.getLongitude());
            String title = airport.getName();
            addMarker(latLng, title);
        }

    }


    private void addMarker(LatLng latLng, String title) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(title));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

    }

    private void zoomToFit(){
//        meters_per_pixel = 156543.03392 * Math.cos(latLng.lat() * Math.PI / 180) / Math.pow(2, zoom)
        if (currentLngLat != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLngLat, 12.0f));
        }
    }

    private void addCircle(LatLng center, String title, double radius, float width) {

        int mStrokeColor = Color.BLACK;
        int mFillColor = Color.RED;

        Circle circle = mMap.addCircle(new CircleOptions()
                .center(center)
                .radius(radius)
                .strokeWidth(width)
                .strokeColor(mStrokeColor)
                .fillColor(mFillColor));
    }

    private void addPolygon(LatLng latLng){
        Polygon mClickablePolygonWithHoles = mMap.addPolygon(new PolygonOptions()
                        .addAll(createPolygon())
//                .addHole(createRectangle(new LatLng(-22, 128), 1, 1))
//                .addHole(createRectangle(new LatLng(-18, 133), 0.5, 1.5))
                        .fillColor(Color.CYAN)
                        .strokeColor(Color.BLUE)
                        .strokeWidth(5)
//                .clickable(mClickabilityCheckbox.isChecked())
        );
    }


     /**
     * Creates a List of LatLngs that form a rectangle with the given dimensions.
     */
    private List<LatLng> createRectangle(LatLng center, double halfWidth, double halfHeight) {
        return Arrays.asList(new LatLng(center.latitude - halfHeight, center.longitude - halfWidth),
                new LatLng(center.latitude - halfHeight, center.longitude + halfWidth),
                new LatLng(center.latitude + halfHeight, center.longitude + halfWidth),
                new LatLng(center.latitude + halfHeight, center.longitude - halfWidth),
                new LatLng(center.latitude - halfHeight, center.longitude - halfWidth));
    }

    private List<LatLng> createPolygon() {
        return Arrays.asList(
                new LatLng(-34, 155),
                new LatLng(-39, 160),
                new LatLng(-45, 151),
                new LatLng(-39, 143),
                new LatLng(-20, 127),
                new LatLng(-10, 151));


    }

}
