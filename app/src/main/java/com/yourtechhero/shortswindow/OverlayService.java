package com.yourtechhero.shortswindow;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

public class OverlayService extends Service {
    private final ArrayList<String> videos = new ArrayList<>();
    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private FrameLayout overlay;
    private WebView player;
    private TextView count;
    private Button play, mute;
    private int index = 0;
    private boolean playing = false, muted = true;
    private int minWidth, minHeight;

    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        return d;
    }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        ArrayList<String> received = intent.getStringArrayListExtra("ids");
        if (received == null || received.isEmpty() || !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf(); return START_NOT_STICKY;
        }
        videos.clear(); videos.addAll(received); index = 0;
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("shorts", "Shorts Window", NotificationManager.IMPORTANCE_LOW));
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note = new Notification.Builder(this, "shorts")
                .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle("Shorts Window is open")
                .setContentText("Tap to change your Shorts").setContentIntent(pi).setOngoing(true).build();
        startForeground(1, note, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        if (overlay == null) createOverlay();
        else loadVideo();
        return START_NOT_STICKY;
    }
    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(13);
        b.setAllCaps(false); b.setMinWidth(0); b.setMinimumWidth(0);
        b.setPadding(dp(4), 0, dp(4), 0);
        b.setBackground(bg(0xff202936, 12));
        return b;
    }
    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        minWidth = dp(210); minHeight = dp(310);
        int width = Math.min(screenW - dp(24), Math.max(minWidth, getSharedPreferences("shorts_window", MODE_PRIVATE).getInt("width", dp(320))));
        int height = Math.min(screenH - dp(140), Math.max(minHeight, getSharedPreferences("shorts_window", MODE_PRIVATE).getInt("height", dp(385))));
        params = new WindowManager.LayoutParams(width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                android.graphics.PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = Math.max(0, Math.min(screenW - width, getSharedPreferences("shorts_window", MODE_PRIVATE).getInt("x", dp(24))));
        params.y = Math.max(0, Math.min(screenH - height, getSharedPreferences("shorts_window", MODE_PRIVATE).getInt("y", dp(165))));

        overlay = new FrameLayout(this);
        overlay.setBackground(bg(0xff090d13, 20));
        overlay.setClipToOutline(true);
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        overlay.addView(column, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(9), dp(5), dp(6), dp(5));
        TextView logo = new TextView(this);
        logo.setText("▶  SHORTS  ·  drag"); logo.setTextSize(12); logo.setTypeface(null, Typeface.BOLD);
        logo.setTextColor(0xffe9f7f4);
        header.addView(logo, new LinearLayout.LayoutParams(0, dp(35), 1));
        Button close = button("×");
        header.addView(close, new LinearLayout.LayoutParams(dp(36), dp(35)));
        close.setOnClickListener(v -> stopSelf());
        column.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));
        header.setOnTouchListener(new View.OnTouchListener() {
            int startX, startY; float downX, downY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    startX = params.x; startY = params.y; downX = e.getRawX(); downY = e.getRawY(); return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.x = Math.max(0, Math.min(getResources().getDisplayMetrics().widthPixels - params.width,
                            startX + Math.round(e.getRawX() - downX)));
                    params.y = Math.max(0, Math.min(getResources().getDisplayMetrics().heightPixels - params.height,
                            startY + Math.round(e.getRawY() - downY)));
                    windowManager.updateViewLayout(overlay, params); return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    getSharedPreferences("shorts_window", MODE_PRIVATE).edit().putInt("x", params.x).putInt("y", params.y).apply(); return true;
                }
                return false;
            }
        });

        FrameLayout videoArea = new FrameLayout(this);
        player = new WebView(this);
        player.setBackgroundColor(Color.BLACK);
        player.getSettings().setJavaScriptEnabled(true);
        player.getSettings().setDomStorageEnabled(true);
        player.getSettings().setMediaPlaybackRequiresUserGesture(true);
        player.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        player.setWebChromeClient(new WebChromeClient());
        player.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, android.webkit.WebResourceRequest req) {
                Uri uri = req.getUrl();
                String host = uri.getHost();
                if (req.isForMainFrame() && host != null && !host.equals("localhost")) {
                    openYouTube(); return true;
                }
                return false;
            }
        });
        videoArea.addView(player, new FrameLayout.LayoutParams(-1, -1));
        // Separate navigation strip leaves the actual embedded player accessible for YouTube controls.
        LinearLayout rail = new LinearLayout(this);
        rail.setOrientation(LinearLayout.VERTICAL); rail.setGravity(Gravity.CENTER);
        rail.setBackground(bg(0xcc131a24, 12));
        Button prev = button("↑"), next = button("↓");
        rail.addView(prev, new LinearLayout.LayoutParams(-1, dp(47)));
        rail.addView(next, new LinearLayout.LayoutParams(-1, dp(47)));
        prev.setOnClickListener(v -> advance(-1)); next.setOnClickListener(v -> advance(1));
        rail.setOnTouchListener(new View.OnTouchListener() {
            float start;
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) { start = event.getY(); return true; }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (Math.abs(event.getY() - start) > dp(24)) advance(event.getY() < start ? 1 : -1);
                    return true;
                }
                return true;
            }
        });
        FrameLayout.LayoutParams railLp = new FrameLayout.LayoutParams(dp(40), dp(110), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        railLp.rightMargin = dp(4); videoArea.addView(rail, railLp);
        column.addView(videoArea, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(dp(7), dp(4), dp(26), dp(5));
        play = button("▶"); mute = button("🔇"); count = new TextView(this);
        count.setTextColor(0xffaebfcb); count.setTextSize(12); count.setGravity(Gravity.CENTER);
        Button open = button("↗ YouTube");
        controls.addView(play, new LinearLayout.LayoutParams(dp(40), dp(39)));
        controls.addView(mute, new LinearLayout.LayoutParams(dp(40), dp(39)));
        controls.addView(count, new LinearLayout.LayoutParams(0, dp(39), 1));
        controls.addView(open, new LinearLayout.LayoutParams(dp(100), dp(39)));
        play.setOnClickListener(v -> {
            playing = !playing; play.setText(playing ? "Ⅱ" : "▶");
            js(playing ? "if(player)player.playVideo()" : "if(player)player.pauseVideo()");
        });
        mute.setOnClickListener(v -> {
            muted = !muted; mute.setText(muted ? "🔇" : "🔊");
            js(muted ? "if(player)player.mute()" : "if(player)player.unMute()");
        });
        open.setOnClickListener(v -> openYouTube());
        column.addView(controls, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView resize = new TextView(this);
        resize.setText("◢"); resize.setTextColor(0xff85d9c8); resize.setTextSize(18); resize.setGravity(Gravity.CENTER);
        resize.setContentDescription("Drag to resize player");
        FrameLayout.LayoutParams handle = new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.RIGHT | Gravity.BOTTOM);
        overlay.addView(resize, handle);
        resize.setOnTouchListener(new View.OnTouchListener() {
            int originalW, originalH; float touchX, touchY;
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    originalW = params.width; originalH = params.height; touchX = e.getRawX(); touchY = e.getRawY(); return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    params.width = Math.max(minWidth, Math.min(getResources().getDisplayMetrics().widthPixels - params.x,
                            originalW + Math.round(e.getRawX() - touchX)));
                    params.height = Math.max(minHeight, Math.min(getResources().getDisplayMetrics().heightPixels - params.y,
                            originalH + Math.round(e.getRawY() - touchY)));
                    windowManager.updateViewLayout(overlay, params); return true;
                }
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    getSharedPreferences("shorts_window", MODE_PRIVATE).edit().putInt("width", params.width).putInt("height", params.height).apply(); return true;
                }
                return false;
            }
        });
        windowManager.addView(overlay, params);
        loadVideo();
    }
    private void js(String command) { if (player != null) player.evaluateJavascript(command, null); }
    private void advance(int direction) {
        index = (index + direction + videos.size()) % videos.size();
        loadVideo();
    }
    private void loadVideo() {
        if (player == null || videos.isEmpty()) return;
        playing = false; muted = true;
        play.setText("▶"); mute.setText("🔇");
        count.setText((index + 1) + " / " + videos.size());
        String id = videos.get(index); // validated as exactly 11 safe YouTube ID characters
        String html = "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'>"
                + "<style>html,body,#video{margin:0;width:100%;height:100%;background:#000;overflow:hidden}</style>"
                + "</head><body><div id='video'></div><script src='https://www.youtube.com/iframe_api'></script>"
                + "<script>var player;function onYouTubeIframeAPIReady(){player=new YT.Player('video',{videoId:'" + id
                + "',playerVars:{playsinline:1,autoplay:0,controls:1,rel:0},events:{onReady:function(e){e.target.mute()}}})}</script></body></html>";
        player.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null);
    }
    private void openYouTube() {
        if (videos.isEmpty()) return;
        js("if(player)player.pauseVideo()");
        playing = false; play.setText("▶");
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/shorts/" + videos.get(index)));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }
    @Override public void onDestroy() {
        if (overlay != null && windowManager != null) {
            windowManager.removeView(overlay); overlay = null;
        }
        if (player != null) {
            player.stopLoading(); player.loadUrl("about:blank"); player.destroy(); player = null;
        }
        super.onDestroy();
    }
}
