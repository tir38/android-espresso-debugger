package com.example.intenttestertester;

import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class IntentsTestRuleTester {

    @Rule
    public IntentsTestRule<MainActivity> mIntentsTestRule = new IntentsTestRule<>(MainActivity.class, true, true);  // launchActivity = true

    @Test
    public void someTestThatReliesOnActivityBeingLaunched() throws Exception {

        // ...

        // will throw:
//        java.lang.NullPointerException: Attempt to invoke virtual method 'void android.support.test.espresso.intent.Intents.internalRelease()' on a null object reference
//        at android.support.test.espresso.intent.Intents.release(Intents.java:140)
//        at android.support.test.espresso.intent.rule.IntentsTestRule.afterActivityFinished(IntentsTestRule.java:68)
//        at android.support.test.rule.ActivityTestRule$ActivityStatement.evaluate(ActivityTestRule.java:260)
//        at org.junit.rules.RunRules.evaluate(RunRules.java:20)
//        at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
//        at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
//        at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
//        at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
//        at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
//        at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
//        at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
//        at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
//        at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
//        at org.junit.runners.Suite.runChild(Suite.java:128)
//        at org.junit.runners.Suite.runChild(Suite.java:27)
//        at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
//        at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
//        at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
//        at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
//        at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
//        at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
//        at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
//        at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
//        at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:54)
//        at android.support.test.runner.AndroidJUnitRunner.onStart(AndroidJUnitRunner.java:240)
//        at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1879)

    }
}
