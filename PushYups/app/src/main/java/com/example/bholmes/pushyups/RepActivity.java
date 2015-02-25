package com.example.bholmes.pushyups;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.bholmes.pushyups.util.FileIO;
import java.io.FileOutputStream;

public class RepActivity extends Activity implements SensorEventListener {
    public static final String LOG_TAG = "PushYup";

    private SensorManager sensorManager;
    private Sensor lightSensor;

    private FileOutputStream luxDataFileStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rep);

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Get a file handle for the lux data
        luxDataFileStream = FileIO.getDataFile("luxData.txt");
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        float lux = event.values[0];

        // Log the lux
        FileIO.logToDataFile(luxDataFileStream, System.currentTimeMillis() + "," + lux + "\n");

        // Display the lux
        TextView luxValue = (TextView)findViewById(R.id.luxValue);
        luxValue.setText(String.valueOf(lux));

        // TODO: Up the reps somehow
        TextView repsValue = (TextView)findViewById(R.id.repsValue);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_rep, menu);
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

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();

        // TODO: Use "Fastest" delay?
        sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            luxDataFileStream.close();
        } catch(Exception e) {
            Log.e(LOG_TAG, "Error closing data output stream: " + e, e);
        }
    }
}
