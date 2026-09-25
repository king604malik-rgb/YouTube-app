package com.yourtechhero.shortswindow;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private EditText links;
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");

    public static ArrayList<String> ids(String input) {
        ArrayList<String> out = new ArrayList<>();
        for (String line : input.split("\\R")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String id = line;
            if (line.startsWith("https://") || line.startsWith("http://")) {
                Uri uri = Uri.parse(line);
                String host = uri.getHost();
                if (host == null) continue;
                host = host.toLowerCase(java.util.Locale.ROOT);
                if (host.equals("youtu.be") || host.equals("www.youtu.be")) {
                    if (uri.getPathSegments().isEmpty()) continue;
                    id = uri.getPathSegments().get(0);
                } else if (host.equals("youtube.com") || host.equals("www.youtube.com") || host.equals("m.youtube.com")) {
                    if (uri.getPathSegments().size() >= 2 && (uri.getPathSegments().get(0).equals("shorts") || uri.getPathSegments().get(0).equals("embed")))
                        id = uri.getPathSegments().get(1);
                    else if (uri.getPath().equals("/watch")) id = uri.getQueryParameter("v");
                    else continue;
                } else continue;
            }
            if (id != null && ID.matcher(id).matches() && !out.contains(id)) out.add(id);
        }
        return out;
    }

    private int dp(int x) { return (int) (x * getResources().getDisplayMetrics().density + .5f); }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(11, 15, 21));
        TextView title = new TextView(this);
        title.setText("SHORTS WINDOW"); title.setTextSize(26); title.setTypeface(null, Typeface.BOLD); title.setTextColor(Color.WHITE);
        root.addView(title);
        TextView sub = new TextView(this);
        sub.setText("Paste public YouTube Shorts links, one per line. Drag the floating player wherever it fits on your home screen.");
        sub.setTextColor(0xffafbbcb); sub.setTextSize(15); sub.setPadding(0, dp(14), 0, dp(20));
        root.addView(sub);
        links = new EditText(this);
        links.setHint("https://youtube.com/shorts/…"); links.setHintTextColor(0xff718094);
        links.setTextColor(Color.WHITE); links.setTextSize(14); links.setGravity(Gravity.TOP);
        links.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        links.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff69d8c0));
        links.setText(getPreferences(0).getString("links", ""));
        root.addView(links, new LinearLayout.LayoutParams(-1, 0, 1));
        Button show = new Button(this); show.setText("Show floating player");
        root.addView(show, new LinearLayout.LayoutParams(-1, dp(58)));
        show.setOnClickListener(v -> {
            ArrayList<String> parsed = ids(links.getText().toString());
            if (parsed.isEmpty()) { Toast.makeText(this, "Add at least one valid YouTube Shorts link", Toast.LENGTH_LONG).show(); return; }
            getPreferences(0).edit().putString("links", links.getText().toString()).apply();
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Allow Display over other apps, then return and tap Show", Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                return;
            }
            Intent intent = new Intent(this, OverlayService.class);
            intent.putStringArrayListExtra("ids", parsed);
            startForegroundService(intent);
            moveTaskToBack(true);
        });
        setContentView(root);
    }
}
