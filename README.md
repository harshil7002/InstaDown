# InstaDo

Current app version: **1.3** (`versionCode` 3).

InstaDo is a native Android application concept for downloading Instagram-related media through a URL paste workflow. The app is designed for mobile-first use with a professional glassmorphism interface, dark/light themes, minimal animations, and swipe navigation between Home, Gallery, and Settings.

## Core behavior

- **Home download workflow:** Paste an Instagram URL or import it from the clipboard, preview the URL type, then save the URL locally or download direct media links with Android DownloadManager.
- **No opening loading page:** The main activity renders the Home page immediately.
- **Local-only storage:** URL history and preview fingerprints are stored on-device in SharedPreferences; downloaded files are sent to `Downloads/InstaDo`.
- **Gallery integration:** The Gallery page reads Android MediaStore so files in the InstaDo folder can appear in both the app and the phone gallery.
- **Permissions:** Settings includes an allow/disallow dialog explaining gallery and storage permissions before requesting Android runtime permissions.
- **Data mismatch indicator:** Repeated URL records are fingerprinted locally so the app can mark when saved preview data differs from an earlier record for the same URL.

> Note: Instagram page URLs often require platform-side extraction, account context, or authorization. This project avoids cloud servers and does not bypass Instagram controls; direct media URLs can be handed to Android DownloadManager, while page URLs are stored and flagged for verification.
