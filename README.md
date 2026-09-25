# Shorts Window

An Android home-screen overlay for YouTube Shorts. Add public Shorts links, grant **Display over other apps**, and tap **Show window**. Drag the header to position it between your home-screen widgets.

The window has play/pause and mute buttons, previous/next controls, and **Open in YouTube** for the current Short. It starts paused and muted. Shorts must allow embedding. This app cannot read your personalized Shorts feed or run inside a conventional launcher widget; the floating window stays above other apps until closed.

## Build and install

On your phone, open the repository's **Actions > Build Android APK > latest successful run**. Under **Artifacts**, download **Shorts-Window-debug-APK**, unzip it, then install `app-debug.apk`. If Android asks, enable installing apps from the file manager you used. Open Shorts Window, paste Shorts URLs (one per line), grant overlay access, and tap **Show floating player**. Drag the header to move it, or drag the lower-right **◢** corner to resize it; the size and position are remembered. You can also open this project in Android Studio and choose **Build > Build APK(s)**.

Requires Android 8.0 or newer and internet. Playback uses the official YouTube IFrame Player. There is no YouTube login or Data API key. A video owner or YouTube can disable embedding, in which case **Open in YouTube** still works. Android shows a notification while the overlay is running.
