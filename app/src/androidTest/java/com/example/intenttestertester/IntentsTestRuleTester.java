package com.example.intenttestertester;

import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IntentsTestRuleTester {

    @Rule
    public IntentsTestRule<MainActivity> mIntentsTestRule = new IntentsTestRule<>(MainActivity.class, true, true);

    @Test
    public void someTestThatReliesOnActivityBeingLaunched() throws Exception {
        // ...

        // will throw:

    }
}
