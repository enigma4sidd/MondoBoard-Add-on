package com.example.sidd.mondo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;


import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.google.android.gms.maps.GoogleMap.*;


public class MapsActivity extends FragmentActivity implements OnMarkerClickListener, View.OnClickListener {


    private static String uniqueID = null;  //unique ID assigned to user
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID";
    LatLng myl;
    Context ctx = this;
    private ProgressDialog pDialog;
    GPSTracker mGPS;
    String po;
    String  routename;
    Polyline line,myroute;
    public static String id1;
    String nam1;
    Double speed;
    // URL to get contacts JSON
    private static String url= "http://iamsidd.eu.pn/mondo.php", url2,url3="http://iamsidd.eu.pn/post.php";
    Databaseops db ;
    int distance;
    String duratn;
    // JSON Node names
    private static final String TAG_ID = "id";
    private static final String TAG_NAME = "name";
    private static final String TAG_lat = "lat";
    private static final String TAG_lon = "lon";
    private static final String TAG_speed = "speed";
    TextView t1,t2,t3,ic_t1,ic_t2,ic_t3,ic_t4;
    ImageView ic_image,ic_image2,ic_image3;
    ListView allrows;
    ToggleButton rb1;
    Button b_route;
    Marker start, finish;
    // contacts JSONArray
    JSONArray contacts = null;
    private HashMap<Integer, Marker> markerList;
    List<LatLng> tracker;
    // Hashmap for ListView
    ArrayList<HashMap<String, String>> contactList;

    HashMap<String, String> contact;
    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
boolean rec_start=false,first=true;
    ViewGroup mapView;
    LayoutInflater inflater;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        allrows = (ListView) findViewById(R.id.listView);
        db = new Databaseops(ctx);
        tracker = new ArrayList<LatLng>();
        contactList = new ArrayList<HashMap<String, String>>();
        markerList = new HashMap<Integer, Marker>();

        t1 = (TextView) findViewById(R.id.texts1);
        t2 = (TextView) findViewById(R.id.texts2);
        t3 = (TextView) findViewById(R.id.texts3);
        rb1 = (ToggleButton) findViewById(R.id.record);
        b_route = (Button) findViewById(R.id.myroute);
        id1 = id(getApplicationContext());
        nam1 =  getUsername();
        inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mapView = (ViewGroup)findViewById(R.id.map1);

                t3.setText("Hello "+nam1);
                setUpMapIfNeeded();
                final Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {

                        setUpMap();
                handler.postDelayed(this, 30000); //now is every 30 sec
            }
        }, 30000);

        mMap.setOnMarkerClickListener(this);
        rb1.setOnClickListener(this);
        b_route.setOnClickListener(this);
}

    private String getUsername() {
        Cursor c = getApplication().getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
        c.moveToFirst();
        String name =  c.getString(c.getColumnIndex("display_name"));

        c.close();
        return name;
    }

    @Override
    protected void onResume() {

        super.onResume();
        new GetContacts().execute();


    }

    public synchronized static String id(Context context) {
        if (uniqueID == null) {
            SharedPreferences sharedPrefs = context.getSharedPreferences(
                    PREF_UNIQUE_ID, Context.MODE_PRIVATE);
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString();
                SharedPreferences.Editor editor = sharedPrefs.edit();
                editor.putString(PREF_UNIQUE_ID, uniqueID);
                editor.commit();
            }
        }
        return uniqueID;
    }
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setInfoView();
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mGPS = new GPSTracker(this);


        if (mGPS.isNetworkAvailable(this)) {
            Log.v("isNetworkAvailable", "= Available");
        } else {
            mGPS.showpopup();
        }
        new GetContacts().execute();
        if (mGPS.canGetLocation) {
       mainoperation();
        }

        else
            mGPS.showSettingsAlert();
       Thread t = new Thread();
       try {
           t.sleep(2000);
       }catch(Exception e){
           e.printStackTrace();
       }
        mainoperation();

    }

    public void mainoperation(){

        mGPS.getLocation();
        mMap.setMyLocationEnabled(true);

        LatLng myLocation = new LatLng(mGPS.getLatitude(), mGPS.getLongitude());

        myl=myLocation;

        if (first==true && (myl.latitude!=0 && myl.longitude!=0))
        {  mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 14));
            mMap.setBuildingsEnabled(true);
            first=false;
        }
        speed = mGPS.getSpeed()*3.6;

        new PostContact().execute();

        t1.setBackgroundColor(0xFF53DEFF);
        t1.setText("My Location : " +myl.toString()+"\nCurrent Speed of travel : " +speed +" kmph ");
        if(rec_start == true && (myl.latitude!=0 && myl.longitude!=0)) {
            tracker.add(myLocation);
            drawmyroute();
        }


    }

    private void drawmyroute() {
        if(myroute!=null)
        myroute.remove();
        PolylineOptions route = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        for (int z = 0; z < tracker.size(); z++) {
            start = mMap.addMarker(new MarkerOptions()
                    .position(tracker.get(0))
                    .title("Start")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start)));

            LatLng point = tracker.get(z);
            route.add(point);
        }
        myroute = mMap.addPolyline(route);
    }

    public void showMarkers()
    {
        for (int i = 0; i < markerList.size(); i++) {

            try {
                markerList.get(i).remove();
                markerList.remove(i);
            }catch (Exception e) {
                e.printStackTrace();
            }

        }


        for (int a = 0; a < contactList.size(); a++) {
            try {
                markerList.put(a, mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(contactList.get(a).get(TAG_lat)), Double.parseDouble(contactList.get(a).get(TAG_lon)))).title(contactList.get(a).get(TAG_NAME)).snippet(contactList.get(a).get(TAG_speed))));
            }catch (ArrayIndexOutOfBoundsException e){ e.printStackTrace();}
        }
    }

public void setInfoView() {

    mMap.setInfoWindowAdapter(new InfoWindowAdapter() {
        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            final View infoview = getLayoutInflater().inflate(R.layout.info_window,
                    null);

            ic_image = (ImageView) infoview.findViewById(R.id.ic_image);
            ic_image2 = (ImageView) infoview.findViewById(R.id.ic_image2);
            ic_image3 = (ImageView) infoview.findViewById(R.id.ic_image3);
            ic_t1 = (TextView) infoview.findViewById(R.id.ic_t1);
            ic_t2 = (TextView) infoview.findViewById(R.id.ic_t2);
            ic_t3 = (TextView) infoview.findViewById(R.id.ic_t3);
            ic_t4 = (TextView) infoview.findViewById(R.id.ic_t4);
            new GetDistance().cancel(true);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(),14));

            String str_result = null;
            t1.setBackgroundColor(0x99F80000 );
            t1.setText(marker.getTitle()+"'s Location : "+marker.getPosition().toString()+"\n"+marker.getTitle()+"'s Current Speed of travel : " +marker.getSnippet() +" kmph ");
            if (distance!=0)
                line.remove();
            distance = 0;
            url2 = "https://maps.googleapis.com/maps/api/directions/json?origin="
                    +mGPS.getLatitude()+","+mGPS.getLongitude()
                    +"&destination="+marker.getPosition().latitude+","+marker.getPosition().longitude+"&mode=cycling";

            try {
                str_result= new GetDistance().execute().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            if (str_result.equals("success")){
                double dist=(double)distance/1000;

                if(distance>1000) {
                    ic_t1.setText("Distance: " +dist+"km");


                }
                else{
                    ic_t1.setText("Distance: " +distance+"m"  );


                }
                Double speeddiff = speed - Double.parseDouble(marker.getSnippet()) ;
                if(speed == 0) {

                        int hr = (int)Math.floor(dist / 12);
                        int min = (int) (((dist/12) % 1) * 60);
                        ic_t2.setText("Time to Location: " + hr + " hr " + min + " min *");
                        ic_t3.setText("Time to catch up to " + marker.getTitle() + ": Never *");
                        ic_t4.setText("*You're not moving. Speed assumed 12 kmph.");

                }
                else {

                        int hr = (int) Math.floor(dist / speed);
                        int min = (int) (((dist/speed) % 1) * 60);
                        ic_t2.setText("Time to Location: " + hr + " hr " + min + " min");

                    if (speeddiff <= 0) {
                        ic_t3.setText("Time to catch up to " + marker.getTitle() + ": Never *");
                        ic_t4.setText("* Your speed is less than " + marker.getTitle() + "s");
                    }
                    else{
                        hr = (int) Math.floor(dist / speeddiff);
                        min = (int) (((dist/speeddiff) % 1) * 60);

                        ic_t3.setText("Time to catch up to " + marker.getTitle() + ": " + hr + " hr " + min + " min");
                    }
                }

            }

            return infoview;
        }
    });



}


    @Override
    public boolean onMarkerClick(Marker marker) {

         if (marker.getTitle().equals("Start") || marker.getTitle().equals("Finish"))
         {}
           else marker.showInfoWindow();

        return true;

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.record: {
                if (rec_start) {
                    rec_start = false;
                    if (tracker.size() - 1 >= 0) {
                        finish = mMap.addMarker(new MarkerOptions()
                                .position(tracker.get(tracker.size() - 1))
                                .title("Finish")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.finish)));
                        saveroutetodb();

                    } else {
                        Toast.makeText(getApplicationContext(), "There was no movement recorded\nPlease start over",
                                Toast.LENGTH_LONG).show();
                        if (myroute != null)
                            myroute.remove();
                        tracker.clear();
                        if (start != null)
                            start.remove();
                    }


                } else {
                    rec_start = true;
                    if (myroute != null)
                        myroute.remove();
                    tracker.clear();
                    if (start != null)
                        start.remove();
                    if (finish != null)
                        finish.remove();
                }
            }
            case R.id.myroute: {


/*
                Cursor CR = db.getInfo(db, "getRoutes");
                String[] columns = new String[]{
                        "_id"
                };

                @SuppressWarnings("deprecation")
                SimpleCursorAdapter myCursorAdapter = new SimpleCursorAdapter(
                        this, R.layout.frame, CR,
                        columns, new int[]{R.id.Routename});
                allrows.setAdapter(myCursorAdapter);
                int visibility = allrows.getVisibility();

                if(visibility==View.GONE)
                     allrows.setVisibility(View.VISIBLE);
*/
            }
            }
        }

    private void saveroutetodb() {


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Title");

// Set up the input
        final EditText input = new EditText(this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                routename = input.getText().toString();

                 boolean b = db.CheckIsDataAlreadyInDBorNot( db,routename);
                if (routename != null && b==false)

                {
                for (int a = 0; a < tracker.size(); a++) {
                    db.putInfo(db, routename,tracker.get(a).latitude,tracker.get(a).longitude);
                                    }

                Toast.makeText(getApplicationContext(), "Your route has been saved",
                        Toast.LENGTH_LONG).show();
            }}
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }




    private class GetContacts extends AsyncTask<Void, Void, String> {
        private volatile boolean running = true;
        @Override
        protected void onCancelled() {
            running = false;
        }
    @Override
   protected void onPreExecute() {

        // Showing progress dialog
        t2.setText("Finding other people...");
   }
            @Override
            protected String doInBackground(Void... arg0) {

                Json sh = new Json();
               contactList.clear();


                // Making a request to url and getting response
                String jsonStr = sh.makeServiceCall(url, Json.GET);

                Log.d("Response: ", "> " + jsonStr);

                if (jsonStr != null) {
                    try {
                        JSONArray jsonArr = new  JSONArray(jsonStr);

                        // Getting JSON Array node
                        //  contacts = jsonObj.getJSONArray(TAG_CONTACTS);

                        for (int i = 0; i < jsonArr.length(); i++) {
                            JSONObject c = jsonArr.getJSONObject(i);

                            String id = c.getString(TAG_ID);
                            String name = c.getString(TAG_NAME);
                            String lat = c.getString(TAG_lat);
                            String lon = c.getString(TAG_lon);
                            String speed = c.getString(TAG_speed);
                            HashMap<String, String> contact = new HashMap<String, String>();
                            if (id.equals(id1))
                            break;
                            else{
                            // adding each child node to HashMap key => value
                            contact.put(TAG_ID, id);
                            contact.put(TAG_NAME, name);
                            contact.put(TAG_lat, lat);
                            contact.put(TAG_lon, lon);
                            contact.put(TAG_speed, speed);
                            // adding contact to contact list
                            contactList.add(contact);

                        }}


                    } catch (JSONException e) {
                        e.printStackTrace();
                        return "fail";
                    }
                } else {
                    Log.e("Json", "Couldn't get any data from the url");
                }



                return "success";}

    @Override
    protected void onPostExecute(String result) {
        if (result=="success")
        { t2.setText(" ");

        showMarkers();
        }

    }

}

    private class PostContact extends AsyncTask<Void, Void, Void> {
        private volatile boolean running = true;
        @Override
        protected void onCancelled() {
            running = false;
        }
        @Override
        protected void onPreExecute() {

            t2.setText("Updating my location...");
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            // Creating service handler class instance


              Json sh = new Json();
              if (myl == null) {
                  cancel(true);
              }
              Double lat = myl.latitude;
              String lat1 = lat.toString();
              Double lon = myl.longitude;
              String lon1 = lon.toString();

              List<NameValuePair> l1 = new ArrayList<NameValuePair>();



              l1.add(new BasicNameValuePair("id", id1));
              l1.add(new BasicNameValuePair("name", nam1));
              l1.add(new BasicNameValuePair("lat", lat1));
              l1.add(new BasicNameValuePair("lon", lon1));
            l1.add(new BasicNameValuePair("speed", ""+speed));
              String jsonStr = sh.makeServiceCall(url3, Json.POST, l1);

           return null;
    }

        @Override
        protected void onPostExecute(Void result) {
            t2.setText("");
        }

    }



    private class GetDistance extends AsyncTask<Void, Void, String> {
        private String jsonStr;

    @Override
   protected void onPreExecute() {
        super.onPreExecute();
        // Showing progress dialog

        t2.setText("Fetching route...");



    }

        @Override
        protected String doInBackground(Void... arg0) {
            // Creating service handler class instance
            Json sh = new Json();

            // Making a request to url and getting response
            String jsonStr = sh.makeServiceCall(url2, Json.GET);

            Log.d("Response: ", "> " + jsonStr);
            if (jsonStr != null) {
                try {
                    JSONObject responseObject = (JSONObject) new JSONTokener(jsonStr).nextValue();
                    this.jsonStr = responseObject.getString("status");
                    JSONArray routesArray = responseObject.getJSONArray("routes");
                    JSONObject route = routesArray.getJSONObject(0);
                    JSONArray legs;
                    JSONObject leg;
                    JSONArray steps;
                    JSONObject dist;
                    JSONObject polyline;
                    JSONObject dis;
                    JSONObject duration;
                    JSONArray points;
                    if (route.has("legs")) {
                        legs = route.getJSONArray("legs");
                        leg = legs.getJSONObject(0);

                        if (leg.has("distance")) {
                            dis= leg.getJSONObject("distance");
                            distance = (Integer)dis.getInt("value");

                        }

                        if (leg.has("duration")) {
                            duration = leg.getJSONObject("duration");
                            duratn = duration.getString("text");
                        }
                        if (route.has("overview_polyline")) {
                            polyline = route.getJSONObject("overview_polyline");
                            po = polyline.getString("points");
                                                   }


                    } else
                        this.jsonStr = "not found";
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            return "success";
        }
        @Override
        protected void onPostExecute(String result) {

        super.onPostExecute(result);



            t2.setText("");

            List<LatLng> list = decodePoly(po);


            PolylineOptions options = new PolylineOptions().width(5).color(Color.RED).geodesic(true);
            for (int z = 0; z < list.size(); z++) {
                LatLng point = list.get(z);
                options.add(point);
            }
             line = mMap.addPolyline(options);
        }

    }
    private List<LatLng> decodePoly(String encoded) {

        List<LatLng> poly = new ArrayList<LatLng>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}





