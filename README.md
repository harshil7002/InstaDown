# InstaDo

Current app version: **1.4** (`versionCode` 4).

InstaDo is a native Android application concept for downloading Instagram-related media through a URL paste workflow. The app is mobile-first, opens directly to Home without a splash/loading page, and keeps all saved records on the device.

## Platform targets

- Android only.
- Minimum SDK: Android 8.0 / API 26.
- Target SDK: Android 16 / API 36.
- Storage: local phone storage only; no cloud storage and no external server for saved files.

## Implemented app flow

- **Home downloader:** large Instagram URL paste box, smart clipboard detection popup, clear button, download actions for video/audio/image, quality selector, preview card, warning for source/preview mismatch, recent downloads, and a local multi-download queue view.
- **Gallery:** reads Android MediaStore and displays InstaDo-only media from `Downloads/InstaDo`, with search, media-type filtering, and newest/oldest/size sorting.
- **Settings:** dark/light/system theme selection, storage stats, clear cache, delete all downloads request, download-folder note, permission management, privacy placeholders, and app preferences for auto-paste, auto-download, WiFi-only downloads, and notifications.
- **Permissions:** in-app allow/deny explanation dialogs for media storage and notifications on newer Android releases.
- **Error handling:** user-facing messages distinguish invalid Instagram URLs, unsupported URLs, private/deleted/unavailable content guidance, and network errors.

## Local storage layout

Direct media downloads are queued through Android DownloadManager under:

```text
Downloads/
└── InstaDo/
    ├── Reels
    ├── Stories
    ├── Posts
    ├── Images
    └── Audio
```

## Important limitation

Instagram page URLs often require source-side extraction, account context, or authorization. This project avoids cloud servers and does not bypass Instagram controls. Direct media URLs can be handed to Android DownloadManager, while Instagram page URLs are previewed and tracked locally so users can verify content before downloading.
