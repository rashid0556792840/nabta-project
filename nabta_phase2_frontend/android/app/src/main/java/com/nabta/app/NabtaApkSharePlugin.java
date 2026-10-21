package com.nabta.app;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@CapacitorPlugin(name = "NabtaApkShare")
public class NabtaApkSharePlugin extends Plugin {
    @PluginMethod
    public void shareInstalledApk(PluginCall call) {
        try {
            Context context = getContext();
            ApplicationInfo info = context.getApplicationInfo();
            File source = new File(info.sourceDir);
            if (!source.isFile() || !source.canRead()) {
                call.reject("The installed Nabta APK is not readable on this device.");
                return;
            }

            File target = new File(context.getCacheDir(), "Nabta.apk");
            try (InputStream input = new FileInputStream(source);
                 OutputStream output = new FileOutputStream(target, false)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
                output.flush();
            }

            Uri uri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", target);
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/vnd.android.package-archive");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.setClipData(ClipData.newUri(context.getContentResolver(), "Nabta APK", uri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser = Intent.createChooser(send, "Share Nabta APK");
            getActivity().startActivity(chooser);
            JSObject result = new JSObject();
            result.put("shared", true);
            call.resolve(result);
        } catch (Exception error) {
            call.reject("Unable to share the installed Nabta APK.", error);
        }
    }
}
