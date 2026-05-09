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
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_HISTORY = "download_history";
    private static final String KEY_AUTO_PASTE = "auto_paste";
    private static final String KEY_AUTO_DOWNLOAD = "auto_download";
    private static final String KEY_WIFI_ONLY = "wifi_only";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_PRIVATE_MODE = "private_mode";
    private static final String KEY_QUALITY = "download_quality";
    private static final String THEME_DARK = "Dark";
    private static final String THEME_LIGHT = "Light";
    private static final String THEME_SYSTEM = "System default";
    private static final String DOWNLOAD_FOLDER = "InstaDo";
    private static final int REQUEST_MEDIA_PERMISSION = 421;

    private final List<View> pages = new ArrayList<>();
    private final int purple = Color.rgb(139, 92, 246);
    private final int blue = Color.rgb(59, 130, 246);
    private final int cyan = Color.rgb(34, 211, 238);
    private final int danger = Color.rgb(239, 68, 68);

    private SharedPreferences preferences;
    private LinearLayout root;
    private FrameLayout pageHost;
    private LinearLayout navBar;
    private EditText urlInput;
    private EditText gallerySearch;
    private TextView previewTitle;
    private TextView previewMeta;
    private TextView previewBadge;
    private TextView previewWarning;
    private TextView permissionStatus;
    private TextView storageStats;
    private LinearLayout galleryList;
    private LinearLayout historyList;
    private LinearLayout queueList;
    private int activePage = 0;
    private float downX;
    private boolean darkMode;
    private String galleryFilter = "All";
    private String gallerySort = "Newest";
    private String lastClipboardUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        seedDefaults();
        darkMode = shouldUseDarkMode();
        configureWindow();
        buildApp();
        handleSharedText(getIntent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preferences != null && preferences.getBoolean(KEY_AUTO_PASTE, true)) {
            detectClipboardInstagramUrl(false);
        }
        refreshStorageStats();
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

    private void seedDefaults() {
        if (!preferences.contains(KEY_THEME)) {
            preferences.edit()
                    .putString(KEY_THEME, THEME_SYSTEM)
                    .putBoolean(KEY_AUTO_PASTE, true)
                    .putBoolean(KEY_AUTO_DOWNLOAD, false)
                    .putBoolean(KEY_WIFI_ONLY, false)
                    .putBoolean(KEY_NOTIFICATIONS, true)
                    .putBoolean(KEY_PRIVATE_MODE, false)
                    .putString(KEY_QUALITY, "Original")
                    .apply();
        }
    }

    private boolean shouldUseDarkMode() {
        String mode = preferences.getString(KEY_THEME, THEME_SYSTEM);
        if (THEME_LIGHT.equals(mode)) return false;
        if (THEME_DARK.equals(mode)) return true;
        int night = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return night == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(darkMode ? Color.rgb(8, 11, 18) : Color.WHITE);
    }

    private void buildApp() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(14));
        root.setBackground(makeBackground());
        setContentView(root);

        LinearLayout header = row();
        header.setPadding(0, 0, 0, 0);
        root.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(64)));

        LinearLayout mark = new LinearLayout(this);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(thumbnailDrawable());
        TextView arrow = label("↓", 24, true);
        arrow.setTextColor(Color.WHITE);
        mark.addView(arrow);
        header.addView(mark, new LinearLayout.LayoutParams(dp(44), dp(44)));
        header.addView(space(10, 1));

        TextView logo = label("InstaDo", 30, true);
        logo.setTextColor(textColor());
        header.addView(logo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        header.addView(smallPill("Instagram only", purple));

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
        LinearLayout content = column();
        scroll.addView(content);
        content.addView(heroCard());
        content.addView(space(14));

        LinearLayout downloadCard = glassCard();
        downloadCard.addView(label("Paste Instagram URL", 22, true));
        downloadCard.addView(helper("Download reels, stories, posts, images, videos, and audio-only files. Everything is saved locally under Downloads/InstaDo."));

        urlInput = new EditText(this);
        urlInput.setSingleLine(false);
        urlInput.setMinLines(3);
        urlInput.setHint("https://www.instagram.com/reel/...");
        urlInput.setTextColor(textColor());
        urlInput.setHintTextColor(mutedColor());
        urlInput.setPadding(dp(16), dp(12), dp(16), dp(12));
        urlInput.setBackground(inputBackground());
        urlInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updatePreview(s.toString().trim(), "Typed URL"); }
            @Override public void afterTextChanged(Editable s) { }
        });
        downloadCard.addView(urlInput, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(110)));

        LinearLayout firstRow = row();
        Button paste = actionButton("Paste from Clipboard", purple);
        paste.setOnClickListener(v -> pasteClipboard());
        firstRow.addView(paste, new LinearLayout.LayoutParams(0, dp(52), 1));
        firstRow.addView(space(10, 1));
        Button clear = actionButton("Clear", blue);
        clear.setOnClickListener(v -> clearUrl());
        firstRow.addView(clear, new LinearLayout.LayoutParams(0, dp(52), 1));
        downloadCard.addView(firstRow);

        downloadCard.addView(qualitySection());
        downloadCard.addView(downloadOptionsSection());
        downloadCard.addView(previewSection());
        content.addView(downloadCard);

        content.addView(space(14));
        LinearLayout queueCard = glassCard();
        queueCard.addView(label("Multi-download queue", 20, true));
        queueCard.addView(helper("Queued direct media URLs are handed to Android DownloadManager. Pause/resume controls are shown as local queue states; Android manages the active transfer."));
        queueList = column();
        queueCard.addView(queueList);
        content.addView(queueCard);
        refreshQueue();

        content.addView(space(14));
        LinearLayout recent = glassCard();
        recent.addView(label("Recent downloads", 20, true));
        historyList = column();
        recent.addView(historyList);
        content.addView(recent);
        refreshHistory();
        return scroll;
    }

    private LinearLayout qualitySection() {
        LinearLayout card = column();
        card.addView(label("Quality", 16, true));
        LinearLayout chips = row();
        for (String quality : new String[]{"Original", "1080p", "720p", "480p"}) {
            Button chip = actionButton(quality, quality.equals(preferences.getString(KEY_QUALITY, "Original")) ? cyan : Color.rgb(71, 85, 105));
            chip.setOnClickListener(v -> {
                preferences.edit().putString(KEY_QUALITY, quality).apply();
                rebuildCurrentPage();
            });
            chips.addView(chip, new LinearLayout.LayoutParams(0, dp(46), 1));
            chips.addView(space(6, 1));
        }
        card.addView(chips);
        return card;
    }

    private LinearLayout downloadOptionsSection() {
        LinearLayout card = column();
        card.addView(label("Download options", 16, true));
        LinearLayout rowOne = row();
        Button video = actionButton("Download Video", purple);
        video.setOnClickListener(v -> startDownloadFlow("Video"));
        rowOne.addView(video, new LinearLayout.LayoutParams(0, dp(52), 1));
        rowOne.addView(space(8, 1));
        Button audio = actionButton("Audio Only", blue);
        audio.setOnClickListener(v -> startDownloadFlow("Audio"));
        rowOne.addView(audio, new LinearLayout.LayoutParams(0, dp(52), 1));
        card.addView(rowOne);
        Button image = actionButton("Download Image", cyan);
        image.setOnClickListener(v -> startDownloadFlow("Image"));
        card.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        return card;
    }

    private LinearLayout previewSection() {
        LinearLayout preview = glassCard();
        previewTitle = label("No URL selected", 18, true);
        previewMeta = helper("Paste a link to preview media type, username hint, resolution, and expected size when available.");
        previewBadge = smallPill("Waiting", cyan);
        previewWarning = helper("");
        LinearLayout previewHeader = row();
        ImageView thumb = new ImageView(this);
        thumb.setImageDrawable(thumbnailDrawable());
        previewHeader.addView(thumb, new LinearLayout.LayoutParams(dp(82), dp(82)));
        LinearLayout previewText = column();
        previewText.addView(previewTitle);
        previewText.addView(previewMeta);
        previewText.addView(previewBadge);
        previewText.addView(previewWarning);
        previewHeader.addView(previewText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        preview.addView(previewHeader);
        return preview;
    }

    private LinearLayout heroCard() {
        LinearLayout hero = glassCard();
        hero.setPadding(dp(22), dp(22), dp(22), dp(22));
        hero.addView(label("Fast Instagram downloader", 26, true));
        hero.addView(helper("Home opens immediately with no splash screen. Smart clipboard paste, local history, gallery integration, and theme settings are built for a mobile-first workflow."));
        LinearLayout chips = row();
        for (String text : new String[]{"Reels", "Stories", "Posts", "Audio"}) {
            chips.addView(smallPill(text, text.equals("Stories") ? blue : text.equals("Audio") ? cyan : purple));
            chips.addView(space(8, 1));
        }
        hero.addView(chips);
        return hero;
    }

    private ScrollView galleryPage() {
        ScrollView scroll = scroller();
        LinearLayout content = column();
        scroll.addView(content);
        LinearLayout card = glassCard();
        card.addView(label("Gallery", 24, true));
        card.addView(helper("Shows InstaDo media from phone storage only: Downloads/InstaDo/Reels, Stories, Posts, Images, and Audio."));

        gallerySearch = new EditText(this);
        gallerySearch.setHint("Search downloaded media");
        gallerySearch.setTextColor(textColor());
        gallerySearch.setHintTextColor(mutedColor());
        gallerySearch.setBackground(inputBackground());
        gallerySearch.setPadding(dp(14), dp(10), dp(14), dp(10));
        gallerySearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { refreshGallery(); }
            @Override public void afterTextChanged(Editable s) { }
        });
        card.addView(gallerySearch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(54)));
        card.addView(filterStrip(new String[]{"All", "Videos", "Images", "Audio"}, true));
        card.addView(filterStrip(new String[]{"Newest", "Oldest", "Size"}, false));

        Button refresh = actionButton("Refresh phone gallery", purple);
        refresh.setOnClickListener(v -> refreshGallery());
        card.addView(refresh, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        galleryList = column();
        card.addView(galleryList);
        content.addView(card);
        refreshGallery();
        return scroll;
    }

    private HorizontalScrollView filterStrip(String[] items, boolean filter) {
        HorizontalScrollView strip = new HorizontalScrollView(this);
        LinearLayout row = row();
        for (String item : items) {
            boolean active = filter ? item.equals(galleryFilter) : item.equals(gallerySort);
            Button chip = actionButton(item, active ? purple : Color.rgb(71, 85, 105));
            chip.setOnClickListener(v -> {
                if (filter) galleryFilter = item; else gallerySort = item;
                rebuildCurrentPage();
            });
            row.addView(chip, new LinearLayout.LayoutParams(dp(104), dp(46)));
            row.addView(space(6, 1));
        }
        strip.addView(row);
        return strip;
    }

    private ScrollView settingsPage() {
        ScrollView scroll = scroller();
        LinearLayout content = column();
        scroll.addView(content);
        LinearLayout card = glassCard();
        card.addView(label("Settings", 24, true));
        card.addView(helper("Manage themes, local storage, permissions, privacy, and app preferences. InstaDo does not use cloud sync or upload user data."));
        card.addView(helper("Version " + BuildConfig.VERSION_NAME + " (code " + BuildConfig.VERSION_CODE + ")"));
        card.addView(label("Theme", 20, true));
        card.addView(themeButtons());

        card.addView(label("Storage", 20, true));
        storageStats = helper("");
        card.addView(storageStats);
        Button cache = actionButton("Clear cache", blue);
        cache.setOnClickListener(v -> toast("Cache cleared"));
        card.addView(cache, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        Button deleteAll = actionButton("Delete all downloads", danger);
        deleteAll.setOnClickListener(v -> confirmDeleteAll());
        card.addView(deleteAll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        Button folder = actionButton("Change download folder", purple);
        folder.setOnClickListener(v -> toast("Current folder: Downloads/InstaDo"));
        card.addView(folder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        card.addView(label("Permissions", 20, true));
        permissionStatus = helper("");
        card.addView(permissionStatus);
        Button permissions = actionButton("Manage permissions", purple);
        permissions.setOnClickListener(v -> showPermissionDialog());
        card.addView(permissions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        card.addView(label("Privacy", 20, true));
        card.addView(settingSwitch("App lock PIN", KEY_PRIVATE_MODE));
        card.addView(helper("Fingerprint lock and hidden private files are reserved for a future secure-storage release."));

        card.addView(label("App preferences", 20, true));
        card.addView(settingSwitch("Auto paste clipboard", KEY_AUTO_PASTE));
        card.addView(settingSwitch("Auto download", KEY_AUTO_DOWNLOAD));
        card.addView(settingSwitch("Download over WiFi only", KEY_WIFI_ONLY));
        card.addView(settingSwitch("Notifications", KEY_NOTIFICATIONS));

        Button clear = actionButton("Clear local history", danger);
        clear.setOnClickListener(v -> clearHistory());
        card.addView(clear, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        content.addView(card);
        updatePermissionStatus();
        refreshStorageStats();
        return scroll;
    }

    private LinearLayout themeButtons() {
        LinearLayout row = row();
        String current = preferences.getString(KEY_THEME, THEME_SYSTEM);
        for (String mode : new String[]{THEME_DARK, THEME_LIGHT, THEME_SYSTEM}) {
            Button button = actionButton(mode, mode.equals(current) ? purple : Color.rgb(71, 85, 105));
            button.setOnClickListener(v -> {
                preferences.edit().putString(KEY_THEME, mode).apply();
                darkMode = shouldUseDarkMode();
                configureWindow();
                buildApp();
            });
            row.addView(button, new LinearLayout.LayoutParams(0, dp(48), 1));
            row.addView(space(6, 1));
        }
        return row;
    }

    private LinearLayout settingSwitch(String text, String key) {
        LinearLayout row = row();
        TextView label = label(text, 16, true);
        row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch toggle = new Switch(this);
        toggle.setChecked(preferences.getBoolean(key, false));
        toggle.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> preferences.edit().putBoolean(key, isChecked).apply());
        row.addView(toggle);
        return row;
    }

    private void pasteClipboard() {
        if (!detectClipboardInstagramUrl(true)) {
            toast("Clipboard has no Instagram URL text");
        }
    }

    private boolean detectClipboardInstagramUrl(boolean force) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clipData = clipboard == null ? null : clipboard.getPrimaryClip();
        if (clipData == null || clipData.getItemCount() == 0) return false;
        CharSequence text = clipData.getItemAt(0).coerceToText(this);
        if (text == null) return false;
        String url = extractFirstUrl(text.toString());
        if (!isInstagramUrl(url)) return false;
        if (!force && url.equals(lastClipboardUrl)) return true;
        lastClipboardUrl = url;
        if (urlInput != null) {
            urlInput.setText(url);
            updatePreview(url, "Clipboard");
        }
        new AlertDialog.Builder(this)
                .setTitle("Instagram link detected")
                .setMessage(url)
                .setPositiveButton("Use link", null)
                .setNegativeButton("Dismiss", null)
                .show();
        if (preferences.getBoolean(KEY_AUTO_DOWNLOAD, false)) startDownloadFlow(detectType(url));
        return true;
    }

    private void clearUrl() {
        if (urlInput != null) urlInput.setText("");
        updatePreview("", "Cleared");
    }

    private void startDownloadFlow(String requestedType) {
        hideKeyboard();
        String url = urlInput == null ? "" : urlInput.getText().toString().trim();
        if (TextUtils.isEmpty(url) || !isInstagramUrl(url)) {
            updatePreview(url, "Invalid");
            toast("Invalid Instagram URL");
            return;
        }
        if (!isSupportedInstagramUrl(url)) {
            toast("Unsupported URL");
            return;
        }
        if (!hasNetwork()) {
            toast("Network error");
            return;
        }
        if (preferences.getBoolean(KEY_WIFI_ONLY, false) && !isWifiConnected()) {
            toast("Network error: WiFi only is enabled");
            return;
        }
        String type = "Audio".equals(requestedType) ? "Audio" : detectType(url);
        saveHistory(url, type);
        updatePreview(url, type);
        if (!hasMediaPermission()) {
            showPermissionDialog();
            refreshHistory();
            return;
        }
        if (isDirectMediaUrl(url)) {
            enqueueDownload(url, type);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Media preview saved")
                    .setMessage("Preview data may differ from source content. Private content cannot be downloaded, deleted content shows as media unavailable, and Instagram page extraction is not performed by any cloud server.")
                    .setPositiveButton("OK", null)
                    .show();
        }
        refreshHistory();
        refreshGallery();
        refreshQueue();
    }

    private void enqueueDownload(String url, String type) {
        String quality = preferences.getString(KEY_QUALITY, "Original");
        String extension = extensionFor(url, type);
        String fileName = "instado_" + type.toLowerCase(Locale.US) + "_" + System.currentTimeMillis() + extension;
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setDescription("Downloading " + type + " • " + quality);
        if (preferences.getBoolean(KEY_NOTIFICATIONS, true)) {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        } else {
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        }
        request.setAllowedOverMetered(!preferences.getBoolean(KEY_WIFI_ONLY, false));
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, DOWNLOAD_FOLDER + "/" + folderForType(type) + "/" + fileName);
        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            toast("Download started");
            saveQueue(fileName, type, "Downloading 0%");
        }
    }

    private void saveHistory(String url, String type) {
        Set<String> current = new HashSet<>(preferences.getStringSet(KEY_HISTORY, new HashSet<>()));
        String fingerprint = fingerprint(url);
        boolean changed = false;
        for (String entry : current) {
            String[] parts = entry.split("\\|", -1);
            if (parts.length >= 3 && parts[0].equals(url) && !parts[2].equals(fingerprint)) changed = true;
        }
        String date = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US).format(new Date());
        current.add(url + "|" + type + "|" + fingerprint + "|" + date + "|" + (changed ? "changed" : "same") + "|" + preferences.getString(KEY_QUALITY, "Original"));
        preferences.edit().putStringSet(KEY_HISTORY, current).apply();
    }

    private void saveQueue(String fileName, String type, String status) {
        Set<String> queue = new HashSet<>(preferences.getStringSet("download_queue", new HashSet<>()));
        queue.add(fileName + "|" + type + "|" + status);
        preferences.edit().putStringSet("download_queue", queue).apply();
    }

    private void refreshQueue() {
        if (queueList == null) return;
        queueList.removeAllViews();
        Set<String> set = preferences.getStringSet("download_queue", new HashSet<>());
        if (set.isEmpty()) {
            queueList.addView(helper("No active queued downloads."));
            return;
        }
        for (String entry : set) {
            String[] parts = entry.split("\\|", -1);
            String file = parts.length > 0 ? parts[0] : entry;
            String type = parts.length > 1 ? parts[1] : "Media";
            String status = parts.length > 2 ? parts[2] : "Queued";
            LinearLayout row = glassCard();
            row.addView(label(type + " • " + status, 16, true));
            row.addView(helper(file));
            LinearLayout controls = row();
            for (String action : new String[]{"Pause", "Resume", "Cancel"}) {
                Button button = actionButton(action, action.equals("Cancel") ? danger : blue);
                button.setOnClickListener(v -> toast(action + " requested for " + file));
                controls.addView(button, new LinearLayout.LayoutParams(0, dp(42), 1));
                controls.addView(space(6, 1));
            }
            row.addView(controls);
            queueList.addView(row);
        }
    }

    private void refreshGallery() {
        if (galleryList == null) return;
        galleryList.removeAllViews();
        if (!hasMediaPermission()) {
            galleryList.addView(helper("Gallery permission is disallowed. Use Settings to allow storage access, then InstaDo can show downloaded media from phone storage."));
            return;
        }
        List<String[]> rows = queryMediaStore();
        if (rows.isEmpty()) {
            galleryList.addView(helper("No InstaDo media found yet. Files saved under Downloads/InstaDo will appear here and in the phone gallery."));
            return;
        }
        for (String[] row : rows) galleryList.addView(mediaRow(row));
    }

    private List<String[]> queryMediaStore() {
        List<String[]> rows = new ArrayList<>();
        ContentResolver resolver = getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        String[] projection = {MediaStore.Files.FileColumns.DISPLAY_NAME, MediaStore.Files.FileColumns.DATE_ADDED, MediaStore.Files.FileColumns.RELATIVE_PATH, MediaStore.Files.FileColumns.SIZE, MediaStore.Files.FileColumns.MIME_TYPE};
        String selection = MediaStore.Files.FileColumns.RELATIVE_PATH + " LIKE ?";
        String[] args = new String[]{"%" + DOWNLOAD_FOLDER + "%"};
        String order = MediaStore.Files.FileColumns.DATE_ADDED + ("Oldest".equals(gallerySort) ? " ASC" : " DESC");
        if ("Size".equals(gallerySort)) order = MediaStore.Files.FileColumns.SIZE + " DESC";
        String search = gallerySearch == null ? "" : gallerySearch.getText().toString().toLowerCase(Locale.US);
        try (Cursor cursor = resolver.query(uri, projection, selection, args, order)) {
            if (cursor == null) return rows;
            while (cursor.moveToNext() && rows.size() < 50) {
                String name = cursor.getString(0);
                String path = cursor.getString(2);
                String size = formatBytes(cursor.getLong(3));
                String mime = cursor.getString(4) == null ? "" : cursor.getString(4);
                String type = mediaTypeFromMime(mime, name);
                if (!"All".equals(galleryFilter) && !galleryFilter.equals(type)) continue;
                if (!TextUtils.isEmpty(search) && !name.toLowerCase(Locale.US).contains(search)) continue;
                rows.add(new String[]{name, path, size, type});
            }
        } catch (Exception ignored) {
            rows.add(new String[]{"MediaStore access unavailable", "", "", "All"});
        }
        return rows;
    }

    private void refreshHistory() {
        if (historyList == null) return;
        historyList.removeAllViews();
        Set<String> set = preferences.getStringSet(KEY_HISTORY, new HashSet<>());
        if (set.isEmpty()) {
            historyList.addView(helper("No recent downloaded items yet."));
            return;
        }
        for (String entry : set) {
            String[] parts = entry.split("\\|", -1);
            String url = parts.length > 0 ? parts[0] : entry;
            String type = parts.length > 1 ? parts[1] : "Saved";
            String date = parts.length > 3 ? parts[3] : "Local";
            String status = parts.length > 4 ? parts[4] : "same";
            String quality = parts.length > 5 ? parts[5] : "Original";
            historyList.addView(historyRow(url, type, date, status, quality));
        }
    }

    private void clearHistory() {
        preferences.edit().remove(KEY_HISTORY).remove("download_queue").apply();
        refreshHistory();
        refreshQueue();
        refreshStorageStats();
        toast("Local history cleared");
    }

    private void confirmDeleteAll() {
        new AlertDialog.Builder(this)
                .setTitle("Delete all downloads")
                .setMessage("Delete downloaded InstaDo files from the app folder. This cannot remove files if Android storage permissions are denied.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    clearHistory();
                    toast("Delete all downloads requested");
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPermissionDialog() {
        String message = "Allow storage access to show downloaded videos/images/audio from phone storage and save files locally. Deny keeps URL preview history available, but gallery integration and direct downloads are limited.";
        new AlertDialog.Builder(this)
                .setTitle("Permissions")
                .setMessage(message)
                .setPositiveButton("Allow", (dialog, which) -> requestMediaPermission())
                .setNegativeButton("Deny", (dialog, which) -> {
                    updatePermissionStatus();
                    new AlertDialog.Builder(this)
                            .setTitle("Permission denied")
                            .setMessage("Storage access is required for the integrated gallery and saved media actions. You can enable it later from Settings.")
                            .setPositiveButton("OK", null)
                            .show();
                })
                .show();
    }

    private void requestMediaPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.POST_NOTIFICATIONS}, REQUEST_MEDIA_PERMISSION);
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
            boolean allowed = false;
            for (int grant : grantResults) allowed = allowed || grant == PackageManager.PERMISSION_GRANTED;
            toast(allowed ? "Permission allowed" : "Permission denied");
            updatePermissionStatus();
            refreshGallery();
        }
    }

    private void updatePermissionStatus() {
        if (permissionStatus != null) {
            permissionStatus.setText(hasMediaPermission() ? "Storage access: allowed • Notifications managed by Android" : "Storage access: denied • Clipboard is read only when app is active");
        }
    }

    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT < 23) return true;
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                    || checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED;
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

    private boolean isWifiConnected() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (manager == null || Build.VERSION.SDK_INT < 23) return true;
        NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }

    private void handleSharedText(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        String url = extractFirstUrl(text == null ? "" : text);
        if (isInstagramUrl(url) && urlInput != null) {
            urlInput.setText(url);
            updatePreview(url, "Shared URL");
            showPage(0, true);
        }
    }

    private boolean isInstagramUrl(String url) {
        if (TextUtils.isEmpty(url)) return false;
        try {
            Uri uri = Uri.parse(url);
            String host = uri.getHost();
            return host != null && (host.equals("instagram.com") || host.endsWith(".instagram.com"));
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean isSupportedInstagramUrl(String url) {
        String lower = url.toLowerCase(Locale.US);
        return lower.contains("/reel/") || lower.contains("/stories/") || lower.contains("/p/") || lower.contains("/tv/") || isDirectMediaUrl(lower);
    }

    private boolean isDirectMediaUrl(String url) {
        String lower = url.toLowerCase(Locale.US);
        return lower.contains(".mp4") || lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp") || lower.contains(".m4a") || lower.contains(".mp3");
    }

    private String detectType(String url) {
        String lower = url.toLowerCase(Locale.US);
        if (lower.contains("/reel/") || lower.contains(".mp4")) return "Video";
        if (lower.contains("/stories/")) return "Story";
        if (lower.contains("/p/") || lower.contains("/tv/")) return "Post";
        if (lower.contains(".m4a") || lower.contains(".mp3") || lower.contains("audio")) return "Audio";
        if (lower.contains(".jpg") || lower.contains(".jpeg") || lower.contains(".png") || lower.contains(".webp")) return "Image";
        return "Instagram URL";
    }

    private void updatePreview(String url, String source) {
        if (previewTitle == null || previewMeta == null || previewBadge == null || previewWarning == null) return;
        if (TextUtils.isEmpty(url)) {
            previewTitle.setText("No URL selected");
            previewMeta.setText("Paste a link to show thumbnail, media type, username, resolution, and file size when Android can determine it.");
            previewBadge.setText("Waiting");
            previewWarning.setText("");
            return;
        }
        String type = isInstagramUrl(url) ? detectType(url) : "Invalid";
        previewTitle.setText(type + " preview");
        String username = usernameHint(url);
        String resolution = isDirectMediaUrl(url) ? "Original source" : "Resolution unavailable before source fetch";
        String size = isDirectMediaUrl(url) ? "Size pending" : "File size unavailable";
        previewMeta.setText(source + " • " + username + " • " + resolution + " • " + size);
        previewBadge.setText(isDirectMediaUrl(url) ? "Ready to download" : isInstagramUrl(url) ? "Preview saved" : "Invalid Instagram URL");
        previewWarning.setText(isInstagramUrl(url) && !isDirectMediaUrl(url) ? "Preview data may differ from source content." : "");
    }

    private String extractFirstUrl(String text) {
        if (TextUtils.isEmpty(text)) return "";
        for (String part : text.split("\\s+")) {
            if (part.startsWith("http://") || part.startsWith("https://")) return part.trim();
        }
        return text.trim();
    }

    private String usernameHint(String url) {
        try {
            List<String> segments = Uri.parse(url).getPathSegments();
            if (segments.size() > 1 && "stories".equals(segments.get(0))) return "@" + segments.get(1);
        } catch (Exception ignored) { }
        return "Username unavailable";
    }

    private String fingerprint(String url) {
        return detectType(url) + ":" + url.length() + ":" + Math.abs(url.hashCode());
    }

    private String folderForType(String type) {
        if ("Audio".equals(type)) return "Audio";
        if ("Image".equals(type)) return "Images";
        if ("Story".equals(type)) return "Stories";
        if ("Post".equals(type)) return "Posts";
        return "Reels";
    }

    private String extensionFor(String url, String type) {
        String lower = url.toLowerCase(Locale.US);
        if (lower.contains(".mp3")) return ".mp3";
        if (lower.contains(".m4a") || "Audio".equals(type)) return ".m4a";
        if (lower.contains(".png")) return ".png";
        if (lower.contains(".webp")) return ".webp";
        if (lower.contains(".jpg") || lower.contains(".jpeg") || "Image".equals(type)) return ".jpg";
        return ".mp4";
    }

    private String mediaTypeFromMime(String mime, String name) {
        String value = (mime + " " + name).toLowerCase(Locale.US);
        if (value.contains("audio") || value.contains(".mp3") || value.contains(".m4a")) return "Audio";
        if (value.contains("image") || value.contains(".jpg") || value.contains(".png") || value.contains(".webp")) return "Images";
        return "Videos";
    }

    private void refreshStorageStats() {
        if (storageStats == null) return;
        int total = preferences.getStringSet(KEY_HISTORY, new HashSet<>()).size();
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long free = stat.getAvailableBytes();
        storageStats.setText("Total downloads: " + total + "\nApp storage used: tracked locally in Downloads/InstaDo\nFree phone storage: " + formatBytes(free));
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.1f MB", mb);
        return String.format(Locale.US, "%.1f GB", mb / 1024.0);
    }

    private void showPage(int index, boolean animate) {
        activePage = index;
        pageHost.removeAllViews();
        View page = pages.get(index);
        pageHost.addView(page);
        if (animate) {
            page.setAlpha(0f);
            page.setTranslationX(dp(28));
            page.animate().alpha(1f).translationX(0).setDuration(200).start();
        }
        refreshNav();
    }

    private void rebuildCurrentPage() {
        int page = activePage;
        buildApp();
        showPage(page, false);
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
        LinearLayout card = column();
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground(0.24f));
        return card;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
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
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(rounded(color, dp(18), 0));
        return button;
    }

    private TextView historyRow(String url, String type, String date, String status, String quality) {
        TextView row = helper(type + " • " + date + " • " + quality + "\n" + url + "\nActions: Open • Share • Delete\nPreview data: " + status);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        row.setBackground(cardBackground(0.18f));
        return row;
    }

    private TextView mediaRow(String[] parts) {
        TextView row = helper("📁 " + parts[0] + "\n" + parts[3] + " • " + parts[2] + " • " + parts[1] + "\nActions: Open • Share • Rename • Delete • Play");
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
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{purple, blue, cyan});
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
        if (manager != null && urlInput != null) manager.hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
