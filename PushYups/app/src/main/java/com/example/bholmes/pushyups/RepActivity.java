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
import android.view.View;
import android.widget.TextView;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;

import java.lang.RuntimeException;
import java.io.FileOutputStream;
import java.util.ArrayList;

import com.example.bholmes.pushyups.util.FileIO;

public class RepActivity extends Activity implements SensorEventListener {
    public static final String LOG_TAG = "PushYup";

    private SensorManager sensorManager;
    private Sensor lightSensor;

    private FileOutputStream luxDataFileStream;

    private static final int CALIBRATION_COUNTDOWN = 3;
    private int countdown = CALIBRATION_COUNTDOWN;

    private static enum State {
        IDLE ("To begin, place your phone underneath the upper centre of your torso. Then click the button to calibrate the hands free pushup detection for your room."),
        UP_COUNTDOWN ("Prepare for yourself to be in an upright pushup position in..."),
        UP_CALIBRATION ("Calibrating UP position, please wait..."),
        DOWN_COUNTDOWN ("Now prepare yourself to be in a downward pushup position in..."),
        DOWN_CALIBRATION ("Calibrating DOWN position, please wait..."),
        COMPLETE ("Calibration completed, you're now ready to begin hands free pushups!"),
        PUSHUPS ("Pump some iron mofucka!!!");

        private String message;

        State(String message) {
            this.message = message;
        }
    }
    private State state = State.IDLE;

    private Calibration up_calibration = new Calibration();
    private Calibration down_calibration = new Calibration();

    TextView reps_value;
    TextView status_text;
    TextView status_message;
    TextView up_calibration_value;
    TextView down_calibration_value;
    TextView calibration_countdown_value;
    Button calibrate_button;

    private int reps = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rep);

        // Set all the views
        reps_value = (TextView)findViewById(R.id.reps_value);
        status_text = (TextView)findViewById(R.id.status_text);
        status_message = (TextView)findViewById(R.id.status_message);
        up_calibration_value = (TextView)findViewById(R.id.up_calibration_value);
        down_calibration_value = (TextView)findViewById(R.id.down_calibration_value);
        calibration_countdown_value = (TextView)findViewById(R.id.calibration_countdown_value);
        calibrate_button = (Button)findViewById(R.id.calibrate_button);

        // Get an instance of the sensor service, and use that to get an instance of
        // a particular sensor.
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        // Get a file handle for the lux data
        luxDataFileStream = FileIO.getDataFile("luxData.txt");

        // Add handler to the calibration button
        calibrate_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state == State.IDLE || state == State.COMPLETE) {
                    begin_calibration();
                } else {
                    cancel_calibration();
                }
            }
        });

        // Initialize the display
        refresh_display();
    }

    // --------------------------------------------------
    // Utils
    // --------------------------------------------------

    private void refresh_display() {
        reps_value.setText(String.valueOf(reps));
        status_text.setText(state.name());
        status_message.setText(state.message);
        up_calibration_value.setText("Average: " + up_calibration.average + " | Max: " + up_calibration.window_max + " | Min: " + up_calibration.window_min);
        down_calibration_value.setText("Average: " + down_calibration.average + " | Max: " + down_calibration.window_max + " | Min: " + down_calibration.window_min);
    }

    private class Calibration {
        public int average = 0;
        public int window_max = 0;
        public int window_min = 0;
        private ArrayList<Integer> light_levels = new ArrayList<Integer>();

        public void calculate() {
            int total_light = 0;
            for (int light_level : light_levels) {
                total_light += light_level;
            }
            average = light_levels.size() < 0 ? total_light / light_levels.size() : 0;
            window_max = (int)(average * 1.25);
            window_min = (int)(average * 0.75);
        }

        public void reset() {
            average = 0;
            window_max = 0;
            window_min = 0;
            light_levels.clear();
        }
    }

    private int generic_countdown(State next_state) {
        if (countdown == 0) {
            countdown = CALIBRATION_COUNTDOWN;
            state = next_state;
            return 0;
        } else {
            calibration_countdown_value.setText("" + countdown);
            countdown -= 1;
        }

        return 1000;
    }

    private int calibration_countdown(State next_state, Calibration calibration) {
        int delay = generic_countdown(next_state);

        if (countdown == CALIBRATION_COUNTDOWN) {
            // TODO: Begin monitoring light levels
        } else if (countdown == 0) {
            // TODO: End monitoring light levels

            // Calculate the final window and update the display
            calibration.calculate();
            refresh_display();
        }

        return delay;
    }

    // --------------------------------------------------
    // Calibration state handlers
    // --------------------------------------------------

    private void begin_calibration() {
        countdown = CALIBRATION_COUNTDOWN;
        state = State.UP_COUNTDOWN;
        up_calibration.reset();
        down_calibration.reset();
        refresh_display();

        timerHandler.postDelayed(timerRunnable, 0);
        calibrate_button.setText("Cancel Calibration");
    }

    private void cancel_calibration() {
        timerHandler.removeCallbacks(timerRunnable);
        state = State.IDLE;
        calibrate_button.setText("Begin Calibration");
        calibration_countdown_value.setText("");
        refresh_display();
    }

    private int handle_up_countdown() {
        return generic_countdown(State.UP_CALIBRATION);
    }

    private int handle_up_calibration() {
        return calibration_countdown(State.DOWN_COUNTDOWN, up_calibration);
    }

    private int handle_down_countdown() {
        return generic_countdown(State.DOWN_CALIBRATION);
    }

    private int handle_down_calibration() {
        return calibration_countdown(State.COMPLETE, down_calibration);
    }

    private int handle_complete() {
        timerHandler.removeCallbacks(timerRunnable);
        calibrate_button.setText("Redo Calibration");
        calibration_countdown_value.setText("");
        refresh_display();
        return 1000;
    }

    private int state_factory() {
        if (state == State.UP_COUNTDOWN) {
            return handle_up_countdown();
        } else if (state == State.UP_CALIBRATION) {
            return handle_up_calibration();
        } else if (state == State.DOWN_COUNTDOWN) {
            return handle_down_countdown();
        } else if (state == State.DOWN_CALIBRATION) {
            return handle_down_calibration();
        } else if (state == State.COMPLETE) {
            return handle_complete();
        }

        throw new RuntimeException("Invalid state " + state + "reached in state factory!");
    }

    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("timer called");
            boolean should_end = state == State.COMPLETE;

            status_text.setText(state.name());
            status_message.setText(state.message);
            int delay = state_factory();

            // Need to check the state as otherwise the timer runs forever...
            if (!should_end) {
                timerHandler.postDelayed(this, delay);
            }
        }
    };

    // --------------------------------------------------
    // Activity handlers
    // --------------------------------------------------

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
        TextView luxValue = (TextView)findViewById(R.id.lux_value);
        luxValue.setText(String.valueOf(lux));

        // TODO: Up the reps somehow
        TextView repsValue = (TextView)findViewById(R.id.reps_value);
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
        // Be sure to unregister the sensor when the activity pauses
        super.onPause();
        cancel_calibration();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cancel_calibration();
        try {
            luxDataFileStream.close();
        } catch(Exception e) {
            Log.e(LOG_TAG, "Error closing data output stream: " + e, e);
        }
    }
}
