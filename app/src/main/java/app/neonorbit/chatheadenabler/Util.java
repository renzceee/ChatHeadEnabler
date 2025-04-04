package app.neonorbit.chatheadenabler;

import static de.robv.android.xposed.XposedHelpers.findMethodExact;

import android.app.ActivityManager;
import android.app.AndroidAppHelper;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import app.neonorbit.chatheadenabler.dex.Constants;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class Util {
  public static void restartApp(Context context) {
    Intent intent = Intent.makeRestartActivityTask(
        Objects.requireNonNull(context.getPackageManager().getLaunchIntentForPackage(
                context.getPackageName()
        )).getComponent()
    );
    context.startActivity(intent);
    Runtime.getRuntime().exit(0);
  }

  public static void runOnApplication(Consumer<Context> consumer, Consumer<Throwable> onFailure) {
    List<XC_MethodHook.Unhook> applicationHookList = new ArrayList<>();
    applicationHookList.addAll(applicationHooks(param -> {
      if (applicationHookList.isEmpty()) return;
      applicationHookList.forEach(XC_MethodHook.Unhook::unhook);
      applicationHookList.clear();
      try {
        if (param.thisObject instanceof Context) {
          consumer.accept((Context) param.thisObject);
        } else {
          consumer.accept((Context) param.args[0]);
        }
      } catch (Throwable throwable) {
        onFailure.accept(throwable);
      }
    }));
  }

  private static List<XC_MethodHook.Unhook> applicationHooks(Consumer<MethodHookParam> consumer) {
    List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
    hooks.add(hookAfter(findMethodExact(Application.class, "onCreate"), consumer));
    hooks.add(hookAfter(findMethodExact(
        Instrumentation.class, "callApplicationOnCreate", Application.class
    ), consumer));
    return hooks;
  }

  public static XC_MethodHook.Unhook hookAfter(Method method, Consumer<MethodHookParam> consumer) {
    return XposedBridge.hookMethod(method, new XC_MethodHook() {
      protected void afterHookedMethod(MethodHookParam param) { consumer.accept(param); }
    });
  }

  public static void runCatching(Runnable runnable, String msg) {
    try {
      runnable.run();
    } catch (Throwable throwable) {
      Log.w(msg + ": [" + throwable.getClass().getName() + "] -> " + throwable.getMessage());
    }
  }

  public static String getPackageVersion(@Nullable Context context, @NonNull String packageName) {
    try {
      if (context == null) context = AndroidAppHelper.currentApplication();
      PackageManager pm = Objects.requireNonNull(context).getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return String.valueOf(pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).getLongVersionCode());
        }
    } catch (Throwable t) {
      return null;
    }
      return packageName;
  }

  public static void applyUnstableHook() {
    XC_MethodReplacement replacement = XC_MethodReplacement.returnConstant(true);
    XposedHelpers.findAndHookMethod(ActivityManager.class, Constants.REFERENCE_METHOD, replacement);
  }

  public static void showToast(@Nullable Context context, @NonNull String toast) {
    try {
      if (context == null) context = AndroidAppHelper.currentApplication();
      Toast.makeText(context, toast, Toast.LENGTH_LONG).show();
    } catch (Throwable ignored) {}
  }

  public static String getTime() {
    Date date = new Date(System.currentTimeMillis());
    return new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(date);
  }

  public static int parseDpi(Context context, int dpi) {
    return (int) TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dpi,
        context.getResources().getDisplayMetrics()
    );
  }
}
