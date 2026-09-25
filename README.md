# Shorts Window

An Android home-screen overlay for YouTube Shorts. Add public Shorts links, grant **Display over other apps**, and tap **Show window**. Drag the header to position it between your home-screen widgets.

The window has play/pause and mute buttons, previous/next controls, and **Open in YouTube** for the current Short. It starts paused and muted. Shorts must allow embedding. This app cannot read your personalized Shorts feed or run inside a conventional launcher widget; the floating window stays above other apps until closed.

## Build and install

Open this folder in Android Studio, let Gradle sync, then choose **Build > Build APK(s)**. Install `app/build/outputs/apk/debug/app-debug.apk` on your S25 Ultra. Open Shorts Window, paste Shorts URLs (one per line), grant overlay access, and tap **Show floating player**.

Requires Android 8.0 or newer and internet. Playback uses the official YouTube IFrame Player. There is no YouTube login or Data API key. A video owner or YouTube can disable embedding, in which case **Open in YouTube** still works. Android shows a notification while the overlay is running.
