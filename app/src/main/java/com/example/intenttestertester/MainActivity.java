package com.example.intenttestertester;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final long TIMER_DURATION = 46; // time in seconds; needs to be longer than MonitoringInstrumentation.START_ACTIVITY_TIMEOUT_SECONDS
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int SAMPLE_MESSAGE = 1;

    private long mStartTime;
    private Handler mGreedyHandler;
    private TextView mTextView;
    private MessageQueue.IdleHandler mIdleHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textview);

        fillUpMessageQueue();
    }


    @Override
    protected void onStop() {
        super.onStop();

        // cleanup
        mGreedyHandler = null;
        mIdleHandler = null;
    }

    /**
     * this is the loop to keep the message queue full
     */
    @SuppressLint("HandlerLeak") // suppress since we are managing Handler lifecycle
    private void fillUpMessageQueue() {

        startTimer();

        mTextView.setText("we are now flooding the MessageQueue with messages to keep it from going idle");

        mGreedyHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (timeLeft() >= 0) {

                    if (timeLeft() % 1000 == 0) {
                        // every second
                        mTextView.setText("we are now flooding the MessageQueue with messages to keep it from going idle for the next: " + timeLeft() + " milliseconds");
                    }

                    // add a new message
                    obtainMessage(SAMPLE_MESSAGE).sendToTarget();
                } else {
                    Log.d(TAG, "done looping; no longer creating new messages, but there may be more still in the message queue");
                }
            }
        };

        // let's make sure that our queue never goes idle (that is the point of all this, after all)
        mIdleHandler = new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                Log.w(TAG, "queue went idle");
                mTextView.setText("MessageQueue is now idle");
                return false;
            }
        };
        Looper.myQueue().addIdleHandler(mIdleHandler);

        // start flooding queue
        mGreedyHandler.obtainMessage(SAMPLE_MESSAGE).sendToTarget();
    }

    /**
     * Start or restart the timer
     */
    private void startTimer() {
        mStartTime = SystemClock.currentThreadTimeMillis();
    }

    /**
     * @return time left on timer, in milliseconds
     */
    private long timeLeft() {
        return (TIMER_DURATION * 1000) - getDuration();
    }

    private long getDuration() {
        if (mStartTime == 0L) { // timer not started yet
            Log.e(TAG, "timer not started yet");
            return -1;
        }

        return SystemClock.currentThreadTimeMillis() - mStartTime;
    }
}