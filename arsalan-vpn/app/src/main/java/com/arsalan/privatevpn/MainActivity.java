package com.arsalan.privatevpn;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.net.VpnService;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.wireguard.android.backend.Tunnel;
import com.wireguard.config.Config;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_IMPORT = 2001;
    private static final int REQ_VPN_PERMISSION = 2002;
    private static final String SLOT_USA = "profile_usa";
    private static final String SLOT_VIETNAM = "profile_vietnam";
    private static final String BIGO_PACKAGE = "sg.bigo.live";

    private final int navy = Color.rgb(17, 28, 68);
    private final int yellow = Color.rgb(224, 218, 7);
    private final int green = Color.rgb(35, 154, 99);
    private final int muted = Color.rgb(99, 107, 123);
    private final int page = Color.rgb(247, 248, 250);

    private SecureConfigStore store;
    private VpnController controller;
    private String selectedSlot = SLOT_USA;
    private String selectedCountry = "United States";
    private boolean connected = false;
    private boolean working = false;

    private Button usaButton;
    private Button vietnamButton;
    private Button importButton;
    private Button connectButton;
    private TextView profileStatus;
    private TextView connectionStatus;
    private TextView connectionSubtext;
    private View statusDot;
    private CheckBox bigoOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(page);
        getWindow().setNavigationBarColor(Color.WHITE);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        store = new SecureConfigStore(this);
        controller = VpnController.get(this);
        setContentView(buildUi());
        refreshProfileUi();
        refreshTunnelState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (controller != null) refreshTunnelState();
    }

    private View buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(page);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(28), dp(22), dp(32));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = text("PRIVATE WIREGUARD", 12, navy, Typeface.BOLD);
        eyebrow.setLetterSpacing(0.16f);
        root.addView(eyebrow);

        TextView title = text("Arsalan VPN", 34, navy, Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = lpMatchWrap();
        titleLp.topMargin = dp(5);
        root.addView(title, titleLp);

        TextView intro = text("Your own encrypted tunnel. Choose a server profile, then connect with one tap.", 16, muted, Typeface.NORMAL);
        intro.setLineSpacing(0, 1.12f);
        LinearLayout.LayoutParams introLp = lpMatchWrap();
        introLp.topMargin = dp(8);
        root.addView(intro, introLp);

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(20), dp(20), dp(20));
        hero.setBackground(roundRect(Color.WHITE, 24, 0xFFE7E9EE, 1));
        LinearLayout.LayoutParams heroLp = lpMatchWrap();
        heroLp.topMargin = dp(24);
        root.addView(hero, heroLp);

        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        statusDot = new View(this);
        statusDot.setBackground(roundRect(0xFFB9BEC8, 99, 0, 0));
        statusRow.addView(statusDot, new LinearLayout.LayoutParams(dp(12), dp(12)));

        connectionStatus = text("Disconnected", 18, navy, Typeface.BOLD);
        LinearLayout.LayoutParams statusTextLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        statusTextLp.leftMargin = dp(10);
        statusRow.addView(connectionStatus, statusTextLp);
        hero.addView(statusRow);

        connectionSubtext = text("Traffic is using your normal connection.", 14, muted, Typeface.NORMAL);
        LinearLayout.LayoutParams subLp = lpMatchWrap();
        subLp.topMargin = dp(7);
        hero.addView(connectionSubtext, subLp);

        TextView choose = text("Choose location", 15, navy, Typeface.BOLD);
        LinearLayout.LayoutParams chooseLp = lpMatchWrap();
        chooseLp.topMargin = dp(26);
        root.addView(choose, chooseLp);

        LinearLayout countries = new LinearLayout(this);
        countries.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams countriesLp = lpMatchWrap();
        countriesLp.topMargin = dp(10);
        root.addView(countries, countriesLp);

        usaButton = countryButton("🇺🇸  USA");
        vietnamButton = countryButton("🇻🇳  Vietnam");
        countries.addView(usaButton, new LinearLayout.LayoutParams(0, dp(54), 1f));
        Space gap = new Space(this);
        countries.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        countries.addView(vietnamButton, new LinearLayout.LayoutParams(0, dp(54), 1f));

        usaButton.setOnClickListener(v -> selectCountry(SLOT_USA, "United States"));
        vietnamButton.setOnClickListener(v -> selectCountry(SLOT_VIETNAM, "Vietnam"));

        LinearLayout configCard = new LinearLayout(this);
        configCard.setOrientation(LinearLayout.VERTICAL);
        configCard.setPadding(dp(18), dp(17), dp(18), dp(17));
        configCard.setBackground(roundRect(Color.WHITE, 18, 0xFFE7E9EE, 1));
        LinearLayout.LayoutParams configLp = lpMatchWrap();
        configLp.topMargin = dp(14);
        root.addView(configCard, configLp);

        TextView configTitle = text("Server profile", 15, navy, Typeface.BOLD);
        configCard.addView(configTitle);
        profileStatus = text("No configuration imported", 13, muted, Typeface.NORMAL);
        LinearLayout.LayoutParams psLp = lpMatchWrap();
        psLp.topMargin = dp(4);
        configCard.addView(profileStatus, psLp);

        importButton = button("Import WireGuard .conf", navy, Color.WHITE, 14, 14);
        LinearLayout.LayoutParams importLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        importLp.topMargin = dp(13);
        configCard.addView(importButton, importLp);
        importButton.setOnClickListener(v -> openConfigFile());

        bigoOnly = new CheckBox(this);
        bigoOnly.setText("Route only BIGO through the VPN");
        bigoOnly.setTextColor(navy);
        bigoOnly.setTextSize(14);
        bigoOnly.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams bigoLp = lpMatchWrap();
        bigoLp.topMargin = dp(18);
        root.addView(bigoOnly, bigoLp);

        TextView bigoHelp = text("Recommended for your use case. Other apps keep using your normal connection.", 12, muted, Typeface.NORMAL);
        LinearLayout.LayoutParams bhLp = lpMatchWrap();
        bhLp.topMargin = dp(2);
        root.addView(bigoHelp, bhLp);

        connectButton = button("CONNECT", yellow, navy, 16, 18);
        LinearLayout.LayoutParams connectLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
        connectLp.topMargin = dp(24);
        root.addView(connectButton, connectLp);
        connectButton.setOnClickListener(v -> toggleConnection());

        TextView securityTitle = text("Security", 15, navy, Typeface.BOLD);
        LinearLayout.LayoutParams secTitleLp = lpMatchWrap();
        secTitleLp.topMargin = dp(28);
        root.addView(securityTitle, secTitleLp);

        TextView security = text("WireGuard encrypts the tunnel. Imported private keys are encrypted on this phone using Android Keystore and app backup is disabled.", 13, muted, Typeface.NORMAL);
        security.setLineSpacing(0, 1.1f);
        LinearLayout.LayoutParams secLp = lpMatchWrap();
        secLp.topMargin = dp(6);
        root.addView(security, secLp);

        TextView note = text("A server profile is still required. The app cannot create a US or Vietnam IP without a real server in that country.", 12, 0xFF7B5B00, Typeface.NORMAL);
        note.setPadding(dp(14), dp(12), dp(14), dp(12));
        note.setBackground(roundRect(0xFFFFF9DA, 14, 0xFFF0E6A8, 1));
        LinearLayout.LayoutParams noteLp = lpMatchWrap();
        noteLp.topMargin = dp(18);
        root.addView(note, noteLp);

        return scroll;
    }

    private void selectCountry(String slot, String country) {
        if (connected || working) {
            toast("Disconnect before changing the server profile.");
            return;
        }
        selectedSlot = slot;
        selectedCountry = country;
        refreshProfileUi();
    }

    private void refreshProfileUi() {
        boolean usa = SLOT_USA.equals(selectedSlot);
        styleCountryButton(usaButton, usa);
        styleCountryButton(vietnamButton, !usa);
        profileStatus.setText(store.has(selectedSlot)
                ? selectedCountry + " profile is ready"
                : "No " + selectedCountry + " configuration imported");
        importButton.setText(store.has(selectedSlot) ? "Replace " + selectedCountry + " profile" : "Import WireGuard .conf");
    }

    private void openConfigFile() {
        if (connected || working) {
            toast("Disconnect before replacing the profile.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, REQ_IMPORT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) return;

        if (requestCode == REQ_IMPORT && data != null && data.getData() != null) {
            importConfig(data.getData());
        } else if (requestCode == REQ_VPN_PERMISSION) {
            connectNow();
        }
    }

    private void importConfig(Uri uri) {
        try {
            String raw = readAll(uri);
            Config.parse(new BufferedReader(new StringReader(raw)));
            store.save(selectedSlot, raw);
            refreshProfileUi();
            toast(selectedCountry + " profile imported securely.");
        } catch (Exception e) {
            toast("That file is not a valid WireGuard configuration.");
        }
    }

    private void toggleConnection() {
        if (working) return;
        if (connected) {
            setWorking(true, "Disconnecting…");
            controller.disconnect((state, error) -> runOnUiThread(() -> {
                setWorking(false, null);
                if (error != null) toast(readableError(error));
                applyState(state);
            }));
            return;
        }

        if (!store.has(selectedSlot)) {
            toast("Import the " + selectedCountry + " WireGuard profile first.");
            return;
        }

        if (bigoOnly.isChecked() && !isPackageInstalled(BIGO_PACKAGE)) {
            toast("BIGO is not installed on this phone.");
            return;
        }

        Intent permission = VpnService.prepare(this);
        if (permission != null) {
            startActivityForResult(permission, REQ_VPN_PERMISSION);
        } else {
            connectNow();
        }
    }

    private void connectNow() {
        try {
            String raw = store.load(selectedSlot);
            if (raw == null) {
                toast("The selected profile could not be loaded.");
                return;
            }
            if (bigoOnly.isChecked()) raw = withIncludedApplication(raw, BIGO_PACKAGE);
            Config config = Config.parse(new BufferedReader(new StringReader(raw)));

            setWorking(true, "Connecting to " + selectedCountry + "…");
            controller.connect(config, (state, error) -> runOnUiThread(() -> {
                setWorking(false, null);
                if (error != null) toast(readableError(error));
                applyState(state);
            }));
        } catch (Exception e) {
            toast(readableError(e));
        }
    }

    private void refreshTunnelState() {
        controller.state((state, error) -> runOnUiThread(() -> {
            if (error == null) applyState(state);
        }));
    }

    private void applyState(Tunnel.State state) {
        connected = state == Tunnel.State.UP;
        statusDot.setBackground(roundRect(connected ? green : 0xFFB9BEC8, 99, 0, 0));
        connectionStatus.setText(connected ? "Connected" : "Disconnected");
        connectionSubtext.setText(connected
                ? (bigoOnly.isChecked() ? "BIGO is routed through " + selectedCountry + "." : "Phone traffic is routed through " + selectedCountry + ".")
                : "Traffic is using your normal connection.");
        connectButton.setText(connected ? "DISCONNECT" : "CONNECT");
        connectButton.setBackground(roundRect(connected ? navy : yellow, 18, 0, 0));
        connectButton.setTextColor(connected ? Color.WHITE : navy);
        bigoOnly.setEnabled(!connected && !working);
        usaButton.setEnabled(!connected && !working);
        vietnamButton.setEnabled(!connected && !working);
        importButton.setEnabled(!connected && !working);
    }

    private void setWorking(boolean value, String message) {
        working = value;
        connectButton.setEnabled(!value);
        if (value && message != null) {
            connectionStatus.setText(message);
            connectionSubtext.setText("Please keep this screen open for a moment.");
        }
        bigoOnly.setEnabled(!value && !connected);
        usaButton.setEnabled(!value && !connected);
        vietnamButton.setEnabled(!value && !connected);
        importButton.setEnabled(!value && !connected);
    }

    private String withIncludedApplication(String raw, String packageName) {
        String lower = raw.toLowerCase();
        if (lower.contains("includedapplications")) return raw;
        int pos = raw.indexOf("[Interface]");
        if (pos < 0) return raw;
        int insertion = pos + "[Interface]".length();
        return raw.substring(0, insertion) + "\nIncludedApplications = " + packageName + raw.substring(insertion);
    }

    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String readAll(Uri uri) throws Exception {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalStateException("Unable to open file");
            byte[] buffer = new byte[8192];
            int count;
            while ((count = in.read(buffer)) != -1) out.write(buffer, 0, count);
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private String readableError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) return "VPN connection failed. Check the server profile and internet connection.";
        return message.length() > 180 ? message.substring(0, 180) : message;
    }

    private Button countryButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(10), 0, dp(10), 0);
        b.setStateListAnimator(null);
        return b;
    }

    private void styleCountryButton(Button b, boolean selected) {
        b.setTextColor(selected ? Color.WHITE : navy);
        b.setBackground(roundRect(selected ? navy : Color.WHITE, 16, selected ? navy : 0xFFDDE1E8, 1));
    }

    private Button button(String label, int background, int textColor, int textSize, int radius) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(textColor);
        b.setTextSize(textSize);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setAllCaps(false);
        b.setStateListAnimator(null);
        b.setBackground(roundRect(background, radius, 0, 0));
        return b;
    }

    private TextView text(String value, int size, int color, int style) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.DEFAULT, style);
        return t;
    }

    private GradientDrawable roundRect(int fill, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), strokeColor);
        return d;
    }

    private LinearLayout.LayoutParams lpMatchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
