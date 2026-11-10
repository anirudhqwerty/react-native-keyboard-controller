package com.keyboardcontrollerfabricexample;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowInsetsController;
import android.view.inspector.WindowInspector;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import com.reactnativekeyboardcontroller.views.EdgeToEdgeViewRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StatusBarStyleTest {
  private final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
  private final List<String> mismatches = new ArrayList<>();
  private Activity activity;

  @Test
  public void statusBarStylesFollowProviderAndModal() throws Exception {
    assertTrue("Requires real platform appearance API", Build.VERSION.SDK_INT >= 31);
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(value -> activity = value);
      assertNotNull(device.wait(Until.findObject(By.desc("Mount provider")), 90000));
      capture("startup-light");
      click("Mount provider");
      assertNotNull(device.wait(Until.findObject(By.desc("Open modal")), 15000));
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
          assertTrue("KeyboardProvider native view must be active", EdgeToEdgeViewRegistry.INSTANCE.get().getActive()));
      check("provider-light", false, false);
      click("Dark content");
      check("provider-dark", true, false);
      click("Open modal");
      assertNotNull(device.wait(Until.findObject(By.desc("Close modal")), 15000));
      check("modal-dark", true, true);
      click("Light content");
      check("modal-light", false, true);
      click("Dark content");
      check("modal-dark-again", true, true);
      click("Close modal");
      assertNotNull(device.wait(Until.findObject(By.desc("Open modal")), 15000));
      click("Light content");
      check("provider-light-again", false, false);
      click("Open modal");
      assertNotNull(device.wait(Until.findObject(By.desc("Close modal")), 15000));
      check("new-modal-light", false, true);
      click("Close modal");
      assertTrue("STATUS_BAR_APPEARANCE_MISMATCH " + mismatches, mismatches.isEmpty());
    }
  }

  private void click(String text) {
    UiObject2 button = device.wait(Until.findObject(By.desc(text)), 15000);
    assertNotNull("Missing button: " + text, button);
    button.click();
    device.waitForIdle();
  }

  private void check(String stage, boolean dark, boolean modal) throws Exception {
    assertNotNull(device.wait(Until.findObject(By.text("Style: " + (dark ? "dark-content" : "light-content"))), 15000));
    device.waitForIdle();
    long deadline = SystemClock.uptimeMillis() + 3000;
    List<String> observed = new ArrayList<>();
    do {
      observed.clear();
      InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
        int windows = 0;
        for (View root : WindowInspector.getGlobalWindowViews()) {
          if (!root.isShown() || root.getWindowInsetsController() == null) continue;
          windows++;
          int appearance = root.getWindowInsetsController().getSystemBarsAppearance();
          boolean platformDark = (appearance & WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS) != 0;
          boolean legacyDark = (root.getSystemUiVisibility() & View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) != 0;
          boolean isActivity = root == activity.getWindow().getDecorView();
          String state = stage + " window=" + (isActivity ? "activity" : "modal") +
              " appearance=" + appearance + " legacyDark=" + legacyDark + " expectedDark=" + dark;
          Log.i("StatusBarRegression", state);
          if (platformDark != dark || (isActivity && Build.VERSION.SDK_INT <= 34 && legacyDark != dark)) observed.add(state);
        }
        if (windows != (modal ? 2 : 1)) observed.add(stage + " expectedWindows=" + (modal ? 2 : 1) + " actual=" + windows);
      });
      if (observed.isEmpty()) break;
      SystemClock.sleep(50);
    } while (SystemClock.uptimeMillis() < deadline);
    mismatches.addAll(observed);
    capture(stage);
  }

  private void capture(String stage) throws Exception {
    String phase = InstrumentationRegistry.getArguments().getString("phase", "after");
    File folder = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir(null), "status-bar-evidence");
    assertTrue(folder.isDirectory() || folder.mkdirs());
    Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
    assertNotNull("Screenshot unavailable", bitmap);
    try (FileOutputStream output = new FileOutputStream(new File(folder, phase + "-api" + Build.VERSION.SDK_INT + "-" + stage + ".png"))) {
      assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
    }
    bitmap.recycle();
  }
}
