# Testing the Tester
Debugging a problem with Espresso and testing support libs.

### The situation

In an Espresso test, if we start an activity` with an `ActivityTestRule`:

```
@Rule
public ActivityTestRule<MainActivity> mActivityTestRule 
			= new ActivityTestRule<>(MainActivity.class, true, true);
```

then `MonitorInstrumentation` and it's parent `Instrumentation` will monitor to ensure that `MainActivity` is started within some defined time (currently 45 seconds). 

If `MainActivity` isn't started wishing the time frame, our test will fail (as it should).

The way `Instrumentation` ensures that our activity is started is by listening for the UI thread's `MessageQueue` to become idle. The assumption is that if `Instrumentation` starts an activity with an Intent and then the MessageQueue becomes idle then our activity must be finished with it's launching (and presumably in the running state). We'll see this is a big, possibly incorrect, assumption.

This listening is accomplished by asking the `MessageQueue` to notify `Instrumentation` when it becomes idle using an IdleHandler:

Instrumentation.java:

```
private void prePerformCreate(Activity activity) {
    if (mWaitingActivities != null) {
        synchronized (mSync) {
            final int N = mWaitingActivities.size();
            for (int i=0; i<N; i++) {
                final ActivityWaiter aw = mWaitingActivities.get(i);
                final Intent intent = aw.intent;
                if (intent.filterEquals(activity.getIntent())) {
                    aw.activity = activity;
                    mMessageQueue.addIdleHandler(new ActivityGoing(aw));
                }
            }
        }
    }
}
```
[see source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/app/Instrumentation.java#1070)

This is accomplished using an extension of the IdleHandler class:

Instrumentation.java:

```
private final class ActivityGoing implements MessageQueue.IdleHandler {
    private final ActivityWaiter mWaiter;

    public ActivityGoing(ActivityWaiter waiter) {
        mWaiter = waiter;
    }

    public final boolean queueIdle() {
        synchronized (mSync) {
            mWaitingActivities.remove(mWaiter);
            mSync.notifyAll();
        }
        return false;
    }
}
```
[see source](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/master/core/java/android/app/Instrumentation.java#1925)


### The Problem

All of this becomes a problem if our `MessageQueue` never becomes idle. It's possible that `MainActivity` is successfully launched but immediately starts adding other `Message`s to the `MessageQueue`. If this is the case, the `MessageQueue` never becomes idle, then `Instrumentation` times out and thinks that our activity stalled during the launch phase. The `Instrumentation` then fails our test and gives us a nice exception:


```
java.lang.RuntimeException: Could not launch intent Intent { act=android.intent.action.MAIN flg=0x14000000 cmp=com.example.intenttestertester/.MainActivity } within 45 seconds. Perhaps the main thread has not gone idle within a reasonable amount of time? There could be an animation or something constantly repainting the screen. Or the activity is doing network calls on creation? See the threaddump logs. For your reference the last time the event queue was idle before your activity launch request was 1454531505219 and now the last time the queue went idle was: 1454531505219. If these numbers are the same your activity might be hogging the event queue.
at android.support.test.runner.MonitoringInstrumentation.startActivitySync(MonitoringInstrumentation.java:362)
at android.support.test.rule.ActivityTestRule.launchActivity(ActivityTestRule.java:219)
at android.support.test.rule.ActivityTestRule$ActivityStatement.evaluate(ActivityTestRule.java:255)
at org.junit.rules.RunRules.evaluate(RunRules.java:20)
at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:325)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:78)
at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:57)
at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runners.Suite.runChild(Suite.java:128)
at org.junit.runners.Suite.runChild(Suite.java:27)
at org.junit.runners.ParentRunner$3.run(ParentRunner.java:290)
at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:71)
at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:288)
at org.junit.runners.ParentRunner.access$000(ParentRunner.java:58)
at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:268)
at org.junit.runners.ParentRunner.run(ParentRunner.java:363)
at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
at android.support.test.internal.runner.TestExecutor.execute(TestExecutor.java:54)
at android.support.test.runner.AndroidJUnitRunner.onStart(AndroidJUnitRunner.java:240)
at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:1879)
```        

This is no bueno! Our activity started just fine!