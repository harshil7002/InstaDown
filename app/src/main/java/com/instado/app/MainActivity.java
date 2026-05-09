package com.instado.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREFS = "instado_storage";
    private static final String KEY_DARK = "dark_mode";
    private static final String KEY_HISTORY = "download_history";
    private static final String DOWNLOAD_FOLDER = "InstaDo";
    private static final int REQUEST_MEDIA_PERMISSION = 421;
    private final List<View> pages = new ArrayList<>();
    private SharedPreferences preferences;
    private LinearLayout root;
    private FrameLayout pageHost;
    private LinearLayout navBar;
    private int activePage = 0;
    private float downX;
    private boolean darkMode;
    private EditText urlInput;
    private TextView previewTitle;
    private TextView previewMeta;
    private TextView previewBadge;
    private TextView permissionStatus;
    private LinearLayout galleryList;
    private LinearLayout historyList;
    private final int purple = Color.rgb(139, 92, 246);
    private final int pink = Color.rgb(236, 72, 153);
    private final int mint = Color.rgb(45, 212, 191);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        darkMode = preferences.getBoolean(KEY_DARK, true);
        configureWindow();
        buildApp();
        handleSharedText(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleSharedText(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            downX = event.getX();
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            float delta = event.getX() - downX;
            if (Math.abs(delta) > dp(96)) {
                if (delta < 0 && activePage < pages.size() - 1) {
                    showPage(activePage + 1, true);
                    return true;
                }
                if (delta > 0 && activePage > 0) {
                    showPage(activePage - 1, true);
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(darkMode ? Color.rgb(8, 11, 18) : Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(true);
        }
    }

    private void buildApp() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(14));
        root.setBackground(makeBackground());
        setContentView(root);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        TextView logo = label("InstaDo", 30, true);
        logo.setTextColor(darkMode ? Color.WHITE : Color.rgb(18, 24, 38));
        header.addView(logo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView pill = smallPill("URL only • Local storage", purple);
        header.addView(pill);

        pageHost = new FrameLayout(this);
        root.addView(pageHost, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        pages.clear();
        pages.add(homePage());
        pages.add(galleryPage());
        pages.add(settingsPage());
        showPage(0, false);

        navBar = new LinearLayout(this);
        navBar.setGravity(Gravity.CENTER);
        navBar.setPadding(dp(8), dp(8), dp(8), dp(8));
        navBar.setBackground(cardBackground(0.36f));
        root.addView(navBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(72)));
        addNavItem("Home", 0);
        addNavItem("Gallery", 1);
        addNavItem("Settings", 2);
        refreshNav();
    }

    private ScrollView homePage() {
        ScrollView scroll = scroller();
        LinearLayout content = column(dp(4));
        scroll.addView(content);

        content.addView(heroCard());
        content.addView(space(14));

        LinearLayout downloadCard = glassCard();
        downloadCard.addView(label("Paste Instagram URL", 22, true));
        downloadCard.addView(helper("Stories, reels, posts, audio and image links are accepted. Direct media URLs are downloaded immediately; Instagram page URLs are saved and checked so you can verify if the preview data changes."));

        urlInput = new EditText(this);
        urlInput.setSingleLine(false);
        urlInput.setMinLines(2);
        urlInput.setHint("https://www.instagram.com/reel/...");
        urlInput.setTextColor(textColor());
        urlInput.setHintTextColor(mutedColor());
        urlInput.setPadding(dp(16), dp(12), dp(16), dp(12));
        urlInput.setBackground(inputBackground());
        downloadCard.addView(urlInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96)));

        LinearLayout row = row();
        Button paste = actionButton("Paste from clipboard", purple);
        paste.setOnClickListener(v -> pasteClipboard());
        row.addView(paste, new LinearLayout.LayoutParams(0, dp(52), 1));
        row.addView(space(10, 1));
        Button download = actionButton("Download", pink);
        download.setOnClickListener(v -> startDownloadFlow());
        row.addView(download, new LinearLayout.LayoutParams(0, dp(52), 1));
        downloadCard.addView(row);

        LinearLayout preview = glassCard();
        previewTitle = label("No URL selected", 18, true);
        previewMeta = helper("Paste a link to generate the same thumbnail preview across Home, Gallery and Settings history.");
        previewBadge = smallPill("Waiting", mint);
        LinearLayout previewHeader = row();
        ImageView thumb = new ImageView(this);
        thumb.setImageDrawable(thumbnailDrawable());
        previewHeader.addView(thumb, new LinearLayout.LayoutParams(dp(78), dp(78)));
        LinearLayout previewText = column(0);
        previewText.addView(previewTitle);
        previewText.addView(previewMeta);
        previewText.addView(previewBadge);
        previewHeader.addView(previewText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        preview.addView(previewHeader);
        downloadCard.addView(preview);
        content.addView(downloadCard);

        content.addView(space(14));
        LinearLayout tips = glassCard();
        tips.addView(label("Fast mobile workflow", 20, true));
        tips.addView(helper("• Swipe left or right to move between pages\n• All records stay in phone storage with SharedPreferences and MediaStore\n• Permission dialogs explain allow/disallow behavior before requesting access\n• The app flags repeated URLs when saved preview data is different"));
        content.addView(tips);
        return scroll;
    }

    private LinearLayout heroCard() {
        LinearLayout hero = glassCard();
        hero.setPadding(dp(22), dp(22), dp(22), dp(22));
        TextView title = label("Instagram downloader built for speed", 26, true);
        hero.addView(title);
        hero.addView(helper("Paste a URL, confirm the local preview, and save media to the InstaDo folder that is visible in your phone gallery."));
        LinearLayout chips = row();
        chips.addView(smallPill("Stories", purple));
        chips.addView(space(8, 1));
        chips.addView(smallPill("Reels", pink));
        chips.addView(space(8, 1));
        chips.addView(smallPill("Posts", mint));
        hero.addView(chips);
        return hero;
    }

    private ScrollView galleryPage() {
        ScrollView scroll = scroller();
        LinearLayout content = column(dp(4));
        scroll.addView(content);
        LinearLayout card = glassCard();
        card.addView(label("Gallery", 24, true));
        card.addView(helper("InstaDo reads the phone MediaStore so downloaded images and videos appear both here and in the native gallery."));
        Button refresh = actionButton("Refresh phone gallery", purple);
        refresh.setOnClickListener(v -> refreshGallery());
        card.addView(refresh, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        galleryList = column(0);
        card.addView(galleryList);
        content.addView(card);
        refreshGallery();
        return scroll;
    }

    private ScrollView settingsPage() {
        ScrollView scroll = scroller();
        LinearLayout content = column(dp(4));
        scroll.addView(content);
        LinearLayout card = glassCard();
        card.addView(label("Settings", 24, true));
        card.addView(helper("Manage local app data, permissions and display mode. No cloud server is used."));

        TextView version = helper("Version " + BuildConfig.VERSION_NAME + " (code " + BuildConfig.VERSION_CODE + ")");
        card.addView(version);

        LinearLayout modeRow = row();
        TextView modeText = label("Dark mode", 18, true);
        modeRow.addView(modeText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch modeSwitch = new Switch(this);
        modeSwitch.setChecked(darkMode);
        modeSwitch.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
            preferences.edit().putBoolean(KEY_DARK, isChecked).apply();
            darkMode = isChecked;
            buildApp();
        });
        modeRow.addView(modeSwitch);
        card.addView(modeRow);

        permissionStatus = helper("");
        card.addView(permissionStatus);
        Button permissions = actionButton("Manage storage permissions", pink);
        permissions.setOnClickListener(v -> showPermissionDialog());
        card.addView(permissions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        Button clear = actionButton("Clear local history", Color.rgb(239, 68, 68));
        clear.setOnClickListener(v -> clearHistory());
        card.addView(clear, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        card.addView(label("Stored URL data", 20, true));
        historyList = column(0);
        card.addView(historyList);
        content.addView(card);
        updatePermissionStatus();
        refreshHistory();
        return scroll;
    }

    private void pasteClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboard == null ? null : clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) {
            toast("Clipboard is empty");
            return;
        }
        CharSequence text = clipData.getItemAt(0).coerceToText(this);
        if (text == null || text.toString().trim().isEmpty()) {
            toast("Clipboard has no URL text");
            return;
        }
        urlInput.setText(text.toString().trim());
        updatePreview(text.toString().trim(), "Clipboard");
    }

    private void startDownloadFlow() {
        hideKeyboard();
        String url = urlInput.getText().toString().trim();
        if (!isInstagramUrl(url)) {
            updatePreview(url, "Invalid");
            toast("Paste a valid Instagram URL first");
            return;
        }
        if (!hasNetwork()) {
            toast("No network connection");
            return;
        }
        if (!hasMediaPermission()) {
            showPermissionDialog();
            saveHistory(url, "Permission pending");
            return;
        }
        saveHistory(url, detectType(url));
        updatePreview(url, detectType(url));
        if (isDirectMediaUrl(url)) {
            enqueueDownload(url);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Instagram page link saved")
                    .setMessage("InstaDo stores this URL locally and can download direct media URLs. If Instagram returns different data for this same URL later, the history will show a changed preview marker.")
                    .setPositiveButton("OK", null)
                    .show();
        }
        refreshHistory();
        refreshGallery();
    }

    private void enqueueDownload(String url) {
        String extension = url.toLowerCase(Locale.US).contains(".mp4") ? ".mp4" : ".jpg";
        String fileName = "instado_" + System.currentTimeMillis() + extension;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("InstaDo download");
        request.setDescription("Saving Instagram media to phone storage");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, DOWNLOAD_FOLDER + "/" + fileName);
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            toast("Download started in Downloads/" + DOWNLOAD_FOLDER);
        }
    }

    private void saveHistory(String url, String type) {
        Set<String> current = new HashSet<>(preferences.getStringSet(KEY_HISTORY, new HashSet<>()));
        String fingerprint = fingerprint(url);
        boolean changed = false;
        for (String entry : current) {
            String[] parts = entry.split("\\|", -1);
            if (parts.length >= 3 && parts[0].equals(url) && !parts[2].equals(fingerprint)) {
                changed = true;
            }
        }
        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date());
        current.add(url + "|" + type + "|" + fingerprint + "|" + date + "|" + (changed ? "changed" : "same"));
        preferences.edit().putStringSet(KEY_HISTORY, current).apply();
    }

    private void refreshGallery() {
        if (galleryList == null) return;
        galleryList.removeAllViews();
        if (!hasMediaPermission()) {
            galleryList.addView(helper("Gallery permission is not allowed. Use Settings to enable it, or downloads will remain managed by Android Download Manager only."));
            return;
        }
        List<String> rows = queryMediaStore();
        if (rows.isEmpty()) {
            galleryList.addView(helper("No InstaDo media found yet. Direct media downloads saved to Downloads/InstaDo will appear here and in the phone gallery."));
            return;
        }
        for (String row : rows) {
            galleryList.addView(mediaRow(row));
        }
    }

    private List<String> queryMediaStore() {
        List<String> rows = new ArrayList<>();
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.RELATIVE_PATH};
        String selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " LIKE ?";
        String[] args = new String[]{"%" + DOWNLOAD_FOLDER + "%"};
        try (Cursor cursor = resolver.query(uri, projection, selection, args, MediaStore.Files.FileColumns.DATE_ADDED + " DESC")) {
            if (cursor == null) return rows;
            while (cursor.moveToNext() && rows.size() < 20) {
                rows.add(cursor.getString(0) + " • " + cursor.getString(2));
            }
        } catch (Exception ignored) {
            rows.add("MediaStore access is unavailable on this device right now.");
        }
        return rows;
    }

    private void refreshHistory() {
        if (historyList == null) return;
        historyList.removeAllViews();
        Set<String> set = preferences.getStringSet(KEY_HISTORY, new HashSet<>());
        if (set.isEmpty()) {
            historyList.addView(helper("No URL data stored yet."));
            return;
        }
        for (String entry : set) {
            String[] parts = entry.split("\\|", -1);
            String url = parts.length > 0 ? parts[0] : entry;
            String type = parts.length > 1 ? parts[1] : "Saved";
            String date = parts.length > 3 ? parts[3] : "Local";
            String status = parts.length > 4 ? parts[4] : "same";
            historyList.addView(historyRow(url, type, date, status));
        }
    }

    private void clearHistory() {
        preferences.edit().remove(KEY_HISTORY).apply();
        refreshHistory();
        toast("Local URL history cleared");
    }

    private void showPermissionDialog() {
        String message = "Allow lets InstaDo show downloaded media from your phone gallery and save files locally. Disallow keeps the app usable for URL history, but gallery integration and direct downloads may be limited.";
        new AlertDialog.Builder(this)
                .setTitle("Storage and gallery permission")
                .setMessage(message)
                .setPositiveButton("Allow", (dialog, which) -> requestMediaPermission())
                .setNegativeButton("Disallow", (dialog, which) -> {
                    updatePermissionStatus();
                    toast("Permission disallowed. You can allow it later in Settings.");
                })
                .show();
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.POST_NOTIFICATIONS}, REQUEST_MEDIA_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_MEDIA_PERMISSION);
        } else {
            updatePermissionStatus();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MEDIA_PERMISSION) {
            boolean allowed = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            toast(allowed ? "Gallery permission allowed" : "Gallery permission disallowed");
            updatePermissionStatus();
            refreshGallery();
        }
    }

    private void updatePermissionStatus() {
        if (permissionStatus != null) {
            permissionStatus.setText(hasMediaPermission() ? "Gallery permission: allowed" : "Gallery permission: disallowed");
        }
    }

    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT < 23) return true;
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasNetwork() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        if (Build.VERSION.SDK_INT >= 23) {
            NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
            return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        }
        return true;
    }

    private void handleSharedText(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (text != null && urlInput != null) {
            urlInput.setText(text.trim());
            updatePreview(text.trim(), "Shared URL");
            showPage(0, true);
        }
    }

    private boolean isInstagramUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        return host != null && (host.equals("instagram.com") || host.endsWith(".instagram.com"));
    }

    private boolean isDirectMediaUrl(String url) {
        String lower = url.toLowerCase(Locale.US);
        return lower.contains(".mp4") || lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp") || lower.contains(".m4a");
    }

    private String detectType(String url) {
        String lower = url.toLowerCase(Locale.US);
        if (lower.contains("/reel/")) return "Reel";
        if (lower.contains("/stories/")) return "Story";
        if (lower.contains("/p/")) return "Post";
        if (lower.contains(".m4a") || lower.contains("audio")) return "Audio";
        if (isDirectMediaUrl(url)) return "Direct media";
        return "Instagram URL";
    }

    private String fingerprint(String url) {
        return detectType(url) + ":" + Math.abs(url.hashCode());
    }

    private void updatePreview(String url, String source) {
        if (previewTitle == null) return;
        previewTitle.setText(detectType(url));
        previewMeta.setText(source + " • " + (isInstagramUrl(url) ? Uri.parse(url).getHost() : "URL needs instagram.com"));
        previewBadge.setText(isDirectMediaUrl(url) ? "Ready to download" : "Saved preview");
    }

    private void showPage(int index, boolean animate) {
        activePage = index;
        pageHost.removeAllViews();
        View page = pages.get(index);
        pageHost.addView(page);
        if (animate) {
            page.setAlpha(0f);
            page.setTranslationX(dp(24));
            page.animate().alpha(1f).translationX(0).setDuration(180).start();
        }
        refreshNav();
    }

    private void addNavItem(String text, int page) {
        TextView item = label(text, 15, true);
        item.setGravity(Gravity.CENTER);
        item.setOnClickListener(v -> showPage(page, true));
        navBar.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
    }

    private void refreshNav() {
        if (navBar == null) return;
        for (int i = 0; i < navBar.getChildCount(); i++) {
            TextView child = (TextView) navBar.getChildAt(i);
            child.setTextColor(i == activePage ? Color.WHITE : mutedColor());
            child.setBackground(i == activePage ? rounded(purple, dp(24), 0) : null);
        }
    }

    private ScrollView scroller() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        return scroll;
    }

    private LinearLayout glassCard() {
        LinearLayout card = column(dp(10));
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground(0.24f));
        return card;
    }

    private LinearLayout column(int gap) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        if (gap > 0) layout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        return layout;
    }

    private LinearLayout row() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        layout.setGravity(Gravity.CENTER_VERTICAL);
        layout.setPadding(0, dp(8), 0, dp(8));
        return layout;
    }

    private TextView label(String text, int sp, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(textColor());
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setIncludeFontPadding(true);
        return view;
    }

    private TextView helper(String text) {
        TextView view = label(text, 14, false);
        view.setTextColor(mutedColor());
        view.setLineSpacing(dp(2), 1.0f);
        return view;
    }

    private TextView smallPill(String text, int color) {
        TextView pill = label(text, 12, true);
        pill.setTextColor(Color.WHITE);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(12), dp(7), dp(12), dp(7));
        pill.setBackground(rounded(color, dp(28), 0));
        return pill;
    }

    private Button actionButton(String text, int color) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(color, dp(18), 0));
        return button;
    }

    private TextView historyRow(String url, String type, String date, String status) {
        TextView row = helper(type + " • " + date + "\n" + url + "\nPreview data: " + status);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(cardBackground(0.18f));
        return row;
    }

    private TextView mediaRow(String text) {
        TextView row = helper("📁 " + text);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(cardBackground(0.18f));
        return row;
    }

    private View space(int height) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height)));
        return space;
    }

    private View space(int width, int height) {
        Space space = new Space(this);
        space.setLayoutParams(new LinearLayout.LayoutParams(dp(width), dp(height)));
        return space;
    }

    private GradientDrawable makeBackground() {
        int[] colors = darkMode
                ? new int[]{Color.rgb(7, 10, 18), Color.rgb(29, 18, 50), Color.rgb(5, 18, 27)}
                : new int[]{Color.rgb(249, 250, 255), Color.rgb(238, 232, 255), Color.rgb(229, 252, 248)};
        return new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
    }

    private GradientDrawable cardBackground(float alpha) {
        int base = darkMode ? Color.WHITE : Color.rgb(255, 255, 255);
        int stroke = darkMode ? Color.argb(80, 255, 255, 255) : Color.argb(120, 139, 92, 246);
        GradientDrawable drawable = rounded(Color.argb((int) (alpha * 255), Color.red(base), Color.green(base), Color.blue(base)), dp(26), stroke);
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable inputBackground() {
        int fill = darkMode ? Color.argb(72, 255, 255, 255) : Color.argb(190, 255, 255, 255);
        GradientDrawable drawable = rounded(fill, dp(18), Color.TRANSPARENT);
        drawable.setStroke(dp(1), darkMode ? Color.argb(70, 255, 255, 255) : Color.argb(90, 139, 92, 246));
        return drawable;
    }

    private GradientDrawable rounded(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != 0) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable thumbnailDrawable() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{purple, pink, mint});
        drawable.setCornerRadius(dp(22));
        return drawable;
    }

    private int textColor() {
        return darkMode ? Color.WHITE : Color.rgb(17, 24, 39);
    }

    private int mutedColor() {
        return darkMode ? Color.rgb(203, 213, 225) : Color.rgb(82, 96, 119);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void hideKeyboard() {
        InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (manager != null && urlInput != null) {
            manager.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
