# Testing the Tester
Debugging a problem with Espresso and testing support libs.

### The Situation

In an Espresso test, if we start an activity with an `ActivityTestRule`:

```
@Rule
public ActivityTestRule<MainActivity> mActivityTestRule 
		= new ActivityTestRule<>(MainActivity.class, true, true);
```

then `MonitoringInstrumentation` and it's parent `Instrumentation` will monitor to ensure that `MainActivity` is started within some defined time (currently 45 seconds). 

If `MainActivity` isn't started within the time frame, our test will fail (as it should).

The way `Instrumentation` ensures that our activity is started is by listening for the UI thread's `MessageQueue` to become idle. The assumption is that if `Instrumentation` starts an activity with an `Intent` and then the `MessageQueue` becomes idle then our activity must be finished with it's launching (and presumably in the running state). We'll see this is a big, possibly incorrect, assumption.

This listening is accomplished by asking the `MessageQueue` to notify `Instrumentation` when it becomes idle using an `IdleHandler`:

Instrumentation.java:

```
private void prePerformCreate(Activity activity) {
    ...
    for (int i = 0; i < mWaitingActivities.size(); i++) {
        final ActivityWaiter activityWaiter = mWaitingActivities.get(i);
        ...
        activityWaiter.activity = activity;
        mMessageQueue.addIdleHandler(new ActivityGoing(activityWaiter));
    }
}
```
[see source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/app/Instrumentation.java#1070)

Understanding the `ActivityWaiter` isn't important. It's basically a wrapper for our Activity. The `ActivityGoing` is the important part. Its an extension of `IdleHandler` class:

Instrumentation.java:

```
private final class ActivityGoing implements MessageQueue.IdleHandler {
    private final ActivityWaiter mWaiter;

    public ActivityGoing(ActivityWaiter waiter) {
        mWaiter = waiter;
    }

    public final boolean queueIdle() {
        ...
        mWaitingActivities.remove(mWaiter);
        ...
    }
}
```
[see source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/app/Instrumentation.java#1925)

Elsewhere in `Instrumentation`, it is continuously checking to ensure that `mWaitingActivities` no longer contain our `ActivityWaiter`. When this happens, `Instrumentation` and thus `MonitoringInstrumentation` "thinks that our activity has been launched.


### The Problem

All of this becomes a problem if the UI thread's `MessageQueue` never becomes idle. It's possible that `MainActivity` is successfully launched but immediately starts adding other `Message`s to the `MessageQueue`. If this is the case, the `MessageQueue` never becomes idle, then `Instrumentation` times out and thinks that our activity stalled during the launch phase. The `Instrumentation` then fails our test and gives us a nice exception:


```
java.lang.RuntimeException: 
Could not launch intent Intent { act=android.intent.action.MAIN flg=0x14000000 cmp=com.example.intenttestertester/.MainActivity } within 45 seconds. 
Perhaps the main thread has not gone idle within a reasonable amount of time? There 
could be an animation or something constantly repainting the screen. Or the activity is 
doing network calls on creation? See the threaddump logs. For your reference the last 
time the event queue was idle before your activity launch request was 1454531505219 and 
now the last time the queue went idle was: 1454531505219. If these numbers are the same 
your activity might be hogging the event queue.

at android.support.test.runner.MonitoringInstrumentation.startActivitySync(MonitoringInstrumentation.java:362)
...
```        

This is no bueno. Our activity started just fine and it's off doing it's job. If one Message was sitting in the MessageQueue taking 45 seconds that would be a problem. But we'd already know that because our actual app would get an ANR Device not responding problem. There's nothing wrong with our Activity needing to add a bunch of Messages to the MessageQueue


### Double Trouble

If the above was bad enough, we'd still get some feedback telling us that  `Instrumentation` thinks our activity stalled. But if we were to replace our `ActivityTestRule` with a subclass `IntentTestRule` then the same problem happens but we get a different exception:


```
java.lang.NullPointerException: 
Attempt to invoke virtual method 
'void android.support.test.espresso.intent.Intents.internalRelease()' on a null object reference

at android.support.test.espresso.intent.Intents.release(Intents.java:140)
...
```

This exception has nothing to do with the actual error. The problem is deep down in the guts of ActivityTestRule (which IntentTestRule subclasses). Looking at a helper class:

```
private class ActivityStatement extends Statement {
	...
	
    @Override
    public void evaluate() throws Throwable {
        try {
            if (mLaunchActivity) {
                mActivity = launchActivity(getActivityIntent());
            }
            mBase.evaluate();
        } finally {
            finishActivity();
            afterActivityFinished();
        }
    }
}
```
[see source](https://android.googlesource.com/platform/frameworks/testing/+/android-support-test/rules/src/main/java/android/support/test/rule/ActivityTestRule.java#243)


Let's focus on that `evaluate` method:


```
try {
    if (mLaunchActivity) {
        mActivity = launchActivity(getActivityIntent()); // (1)
    }
    mBase.evaluate();
} finally {
    finishActivity();
    afterActivityFinished(); // (2)
}
```

In (1) we expect to possibly see our *java.lang.RuntimeException: Could not launch intent* exception. The problem is that in (2) we'll actually see an NPE that is collateral damage of the timeout problem.


The trouble starts because an instance of `Intents` class was never initialized [here](https://android.googlesource.com/platform/frameworks/testing/+/android-support-test/espresso/intents/src/main/java/android/support/test/espresso/intent/Intents.java#127). I'll let you dig through the source yourself, but because an instance was never created, we get an NPE when `afterActivityFinished()` tries to release this instance [here](https://android.googlesource.com/platform/frameworks/testing/+/android-support-test/espresso/intents/src/main/java/android/support/test/espresso/intent/Intents.java#139). 

### So what's the solution:
If we let Espresso launch our Activity with the IntentTestRule by using either of these constructors then the `Intents` class won't get initialized until **after** our activity has launched. This is the problem. Remember `Instrumentation` thinks that our activity never got launched, it thinks that it stalled.

```
@Rule
public IntentsTestRule<MainActivity> mIntentsTestRule 
		= new IntentsTestRule<>(MainActivity.class);

// or 

@Rule
public IntentsTestRule<MainActivity> mIntentsTestRule 
		= new IntentsTestRule<>(MainActivity.class, true, true);  // launchActivity = true
```

The only way to fix this is to initialize `Intents` ourselves and then manually launch our activity:

```
@Rule
public IntentsTestRule<MainActivity> mIntentsTestRule 
		= new IntentsTestRule<>(MainActivity.class, true, false);  // launchActivity = false

@Test
public void someTest() throws Exception {
    Intents.init();
    mIntentsTestRule.launchActivity(new Intent());
    // the rest of the test
}
```

We'll still see the time-out RuntimeException if we fill up the MessageQueue, but at least we'll know what's going wrong. Of course, manually launching our Activity rearranges the test lifecycle. All of this is well described [here](https://jabknowsnothing.wordpress.com/2015/11/05/activitytestrule-espressos-test-lifecycle/).


### Visualize
This sample app is meant to demonstrate these two problems. It includes the following important classes:

* **MainActivity.java** which during `onCreate()` starts dumping messages to the MessageQueue
* **ActivityTestRuleTester.java** which contains one blank test and an ActivityTestRule to start MainActivity
* **IntentsTestRuleTester.java** which contains one blank test and an IntentTestRule to start MainActivity.