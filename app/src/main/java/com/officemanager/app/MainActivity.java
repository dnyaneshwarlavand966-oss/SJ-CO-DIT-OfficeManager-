package com.officemanager.app;

import android.app.Activity;
import android.os.Build;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayOutputStream;

public class MainActivity extends Activity {
    private static final int REQUEST_SELFIE = 7101;
    private WebView webView;
    private boolean selfiePending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ReminderScheduler.scheduleDaily(this);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 8201);
        }
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new OfficeBridge(), "OfficeBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class OfficeBridge {
        @JavascriptInterface
        public boolean hasOwnerPin() {
            return getSharedPreferences("owner_security", MODE_PRIVATE).contains("pin_hash");
        }

        @JavascriptInterface
        public boolean saveOwnerPin(String pin) {
            if (pin == null || !pin.matches("[0-9]{6}")) return false;
            try {
                byte[] salt = new byte[16];
                new java.security.SecureRandom().nextBytes(salt);
                byte[] hash = ownerPinHash(salt, pin);
                return getSharedPreferences("owner_security", MODE_PRIVATE).edit()
                        .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
                        .putString("pin_hash", Base64.encodeToString(hash, Base64.NO_WRAP))
                        .putInt("failed_attempts", 0).putLong("lock_until", 0L).commit();
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean verifyOwnerPin(String pin) {
            if (pin == null || !pin.matches("[0-9]{6}")) return false;
            android.content.SharedPreferences prefs = getSharedPreferences("owner_security", MODE_PRIVATE);
            long lockUntil = prefs.getLong("lock_until", 0L);
            if (System.currentTimeMillis() < lockUntil) return false;
            String saltText = prefs.getString("pin_salt", null);
            String hashText = prefs.getString("pin_hash", null);
            if (saltText == null || hashText == null) return false;
            try {
                byte[] salt = Base64.decode(saltText, Base64.NO_WRAP);
                byte[] expected = Base64.decode(hashText, Base64.NO_WRAP);
                boolean valid = java.security.MessageDigest.isEqual(expected, ownerPinHash(salt, pin));
                android.content.SharedPreferences.Editor edit = prefs.edit();
                if (valid) {
                    edit.putInt("failed_attempts", 0).putLong("lock_until", 0L);
                } else {
                    int attempts = prefs.getInt("failed_attempts", 0) + 1;
                    edit.putInt("failed_attempts", attempts);
                    if (attempts >= 5) edit.putLong("lock_until", System.currentTimeMillis() + 60000L).putInt("failed_attempts", 0);
                }
                edit.apply();
                return valid;
            } catch (Exception e) {
                return false;
            }
        }

        private byte[] ownerPinHash(byte[] salt, String pin) throws Exception {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            return digest.digest(pin.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        @JavascriptInterface
        public boolean scheduleTeaReminder(String time) {
            if (time == null || !time.matches("([01]?[0-9]|2[0-3]):[0-5][0-9]")) return false;
            try {
                String[] parts = time.split(":");
                return ReminderScheduler.scheduleDaily(MainActivity.this,
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public void printReport() {
            runOnUiThread(() -> {
                PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                if (printManager != null && webView != null) {
                    PrintAttributes attributes = new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                            .build();
                    printManager.print("SJ-CO-DIT-Office-Report",
                            webView.createPrintDocumentAdapter("SJ-CO-DIT-Office-Report"), attributes);
                }
            });
        }

        @JavascriptInterface
        public void takeSelfie() {
            runOnUiThread(() -> {
                if (selfiePending) return;
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (intent.resolveActivity(getPackageManager()) == null) {
                    if (webView != null) webView.evaluateJavascript("selfieCaptured('')", null);
                    return;
                }
                selfiePending = true;
                try {
                    startActivityForResult(intent, REQUEST_SELFIE);
                } catch (Exception e) {
                    selfiePending = false;
                    if (webView != null) webView.evaluateJavascript("selfieCaptured('')", null);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_SELFIE) return;
        selfiePending = false;
        String result = "";
        if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
            Object image = data.getExtras().get("data");
            if (image instanceof Bitmap) {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                ((Bitmap) image).compress(Bitmap.CompressFormat.JPEG, 65, stream);
                result = "data:image/jpeg;base64," + Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
            }
        }
        if (webView != null) {
            String safe = result.isEmpty() ? "" : result;
            webView.evaluateJavascript("selfieCaptured('" + safe + "')", null);
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
