package cn.cengke.campus;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONTokener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String SCHOOLWIDE_URL = "https://ehallapp.nju.edu.cn/jwapp/sys/kcbcx/*default/index.do#/qxkcb01";
    private static final String PERSONAL_URL = "https://ehallapp.nju.edu.cn/jwapp/sys/wdkb/*default/index.do";
    private WebView webView;
    private TextView status;
    private String lastJson;
    private boolean allReading;
    private boolean stopRequested;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(247, 248, 244));
        getWindow().setNavigationBarColor(Color.rgb(247, 248, 244));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(247, 248, 244));
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.VERTICAL);
        toolbar.setPadding(16, 10, 16, 8);
        TextView title = new TextView(this);
        title.setText("蹭课网 · 南大课表导入原型 v0.3.0");
        title.setTextSize(18);
        title.setTextColor(Color.rgb(39, 88, 70));
        toolbar.addView(title);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button openSchoolwide = button("打开全校课表");
        Button openPersonal = button("打开我的课表");
        Button readPage = button("读取当前页");
        Button readAll = button("读取全部页");
        Button stopAll = button("停止读取");
        Button saveJson = button("保存 JSON");
        Button shareJson = button("分享 JSON");
        actions.addView(openSchoolwide, weightParams());
        actions.addView(openPersonal, weightParams());
        actions.addView(readPage, weightParams());
        actions.addView(readAll, weightParams());
        actions.addView(stopAll, weightParams());
        actions.addView(saveJson, weightParams());
        toolbar.addView(actions);
        // 分享按钮单独占一行，避免在手机窄屏上与分页按钮挤在一起。
        toolbar.addView(shareJson, new LinearLayout.LayoutParams(-1, -2));
        status = new TextView(this);
        status.setText("原型说明：请手动登录南大教务系统，进入课表列表后再点击“读取当前页”。应用不会读取或提交密码。");
        status.setTextSize(13);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, 8, 0, 0);
        toolbar.addView(status);
        root.addView(toolbar);
        webView = new WebView(this);
        configureWebView();
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        openSchoolwide.setOnClickListener(v -> loadSchoolUrl(SCHOOLWIDE_URL));
        openPersonal.setOnClickListener(v -> loadSchoolUrl(PERSONAL_URL));
        readPage.setOnClickListener(v -> readCurrentPage());
        readAll.setOnClickListener(v -> readAllPages());
        stopAll.setOnClickListener(v -> stopAllPages());
        saveJson.setOnClickListener(v -> saveJson());
        shareJson.setOnClickListener(v -> shareJson());
        loadSchoolUrl(SCHOOLWIDE_URL);
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(12);
        button.setAllCaps(false);
        return button;
    }

    private LinearLayout.LayoutParams weightParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.setMargins(2, 0, 2, 0);
        return p;
    }

    private void configureWebView() {
        webView.setBackgroundColor(Color.WHITE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.getSettings().setBlockNetworkLoads(false);
        webView.getSettings().setMixedContentMode(android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !isAllowedSchoolUrl(request.getUrl());
            }
            @Override public void onPageFinished(WebView view, String url) {
                status.setText("已打开：" + url + "\n请完成登录并进入课表列表；确认页面显示课程后点击“读取当前页”。");
            }
        });
    }

    private boolean isAllowedSchoolUrl(Uri uri) {
        if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return false;
        String host = uri.getHost();
        return host != null && (host.equals("nju.edu.cn") || host.endsWith(".nju.edu.cn"));
    }

    private void loadSchoolUrl(String url) {
        status.setText("正在打开南大页面……如果需要登录，请在页面内手动完成。");
        webView.loadUrl(url);
    }

    private void readCurrentPage() {
        status.setText("正在检查当前页面的标准课表列表……");
        String extractor = readAsset("extract.js");
        String probe = readAsset("mobile-probe.js");
        String script = "(function(){try{" + extractor + "\nreturn " + probe + "}catch(e){return JSON.stringify({ok:false,error:'读取脚本异常：'+e.message});}})();";
        webView.evaluateJavascript(script, result -> {
            String json = decodeJavascriptString(result);
            try {
                org.json.JSONObject object = new org.json.JSONObject(json);
                boolean ok = object.optBoolean("ok", false);
                int count = object.optInt("count", 0);
                if (ok) {
                    org.json.JSONObject data = object.optJSONObject("data");
                    lastJson = data == null ? null : data.toString();
                    status.setText("读取成功：当前页 " + count + " 个教学班。结果暂存在应用私有目录，点击“保存 JSON”可写入文件。");
                } else {
                    lastJson = null;
                    status.setText("读取失败：" + object.optString("error", "未找到标准课表列表") + "\n请确认已进入课程列表，并把表头完整显示出来。");
                }
            } catch (Exception error) {
                status.setText("读取结果解析失败：" + error.getMessage());
            }
        });
    }

    private void readAllPages() {
        if (allReading) {
            status.setText("正在读取全部页面，请等待当前页完成；如需中止请点击“停止读取”。");
            return;
        }
        allReading = true;
        stopRequested = false;
        lastJson = null;
        status.setText("正在准备分页读取，请先确保当前页面在第 1 页且筛选条件不变……");
        String extractor = readAsset("extract.js");
        String paginator = readAsset("paginate.js");
        String mobilePaginator = readAsset("mobile-paginate.js");
        String script = "(function(){try{" + extractor + "\n" + paginator + "\n" + mobilePaginator +
                "\nreturn window.cengkeStartMobilePagination();}catch(e){return JSON.stringify({started:false,error:'读取脚本异常：'+e.message});}})();";
        webView.evaluateJavascript(script, result -> {
            String started = decodeJavascriptString(result);
            try {
                org.json.JSONObject object = new org.json.JSONObject(started);
                if (!object.optBoolean("started", false)) {
                    allReading = false;
                    status.setText("无法开始分页读取：" + object.optString("error", "未知错误"));
                    return;
                }
                pollAllPages();
            } catch (Exception error) {
                allReading = false;
                status.setText("分页读取启动失败：" + error.getMessage());
            }
        });
    }

    private void pollAllPages() {
        if (!allReading) return;
        webView.evaluateJavascript("JSON.stringify({result:window.__cengkeAllResult||'',progress:window.__cengkeAllProgress||''})", raw -> {
            String decoded = decodeJavascriptString(raw);
            try {
                org.json.JSONObject state = new org.json.JSONObject(decoded);
                String result = state.optString("result", "");
                String progress = state.optString("progress", "");
                if (!progress.isBlank()) status.setText(progress);
                if (!result.isBlank()) {
                    allReading = false;
                    org.json.JSONObject object = new org.json.JSONObject(result);
                    lastJson = object.toString();
                    boolean complete = object.optBoolean("complete", false);
                    int count = object.optInt("rows", 0);
                    if (object.has("rows") && object.opt("rows") instanceof org.json.JSONArray) count = object.optJSONArray("rows").length();
                    status.setText((complete ? "全部读取成功：" : "分页读取已停止：") + count + " 个教学班。点击“保存 JSON”写入文件。\n" + object.optString("reason", ""));
                    return;
                }
            } catch (Exception error) {
                allReading = false;
                status.setText("分页结果解析失败：" + error.getMessage());
                return;
            }
            handler.postDelayed(this::pollAllPages, 500);
        });
    }

    private void stopAllPages() {
        if (!allReading) {
            status.setText("当前没有正在进行的分页读取。");
            return;
        }
        stopRequested = true;
        status.setText("正在停止分页读取，并保存已经读取的页面……");
        webView.evaluateJavascript("window.__cengkeAllStop=true;", null);
    }

    private String decodeJavascriptString(String value) {
        try {
            Object decoded = new JSONTokener(value).nextValue();
            return decoded instanceof String ? (String) decoded : value;
        } catch (Exception ignored) { return value; }
    }

    private void saveJson() {
        if (lastJson == null || lastJson.isBlank()) { status.setText("还没有可保存的结果，请先点击“读取当前页”。"); return; }
        try {
            File dir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (dir == null) dir = getFilesDir();
            if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("无法创建应用文件目录");
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            File file = new File(dir, "nju-timetable-" + stamp + ".json");
            try (FileOutputStream output = new FileOutputStream(file)) { output.write(lastJson.getBytes(StandardCharsets.UTF_8)); }
            status.setText("已保存 JSON：" + file.getAbsolutePath() + "\n文件只写入本应用目录，未上传云端。");
        } catch (Exception error) { status.setText("保存失败：" + error.getMessage()); }
    }

    private void shareJson() {
        if (lastJson == null || lastJson.isBlank()) {
            status.setText("还没有可分享的结果，请先读取课表并保存 JSON。");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            status.setText("当前原型的系统分享需要 Android 10 及以上，请先使用“保存 JSON”。");
            return;
        }
        Uri uri = null;
        try {
            String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date());
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, "nju-timetable-" + stamp + ".json");
            values.put(MediaStore.Downloads.MIME_TYPE, "application/json");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/蹭课网");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("无法创建 Download 文件");
            try (OutputStream output = getContentResolver().openOutputStream(uri)) {
                if (output == null) throw new IllegalStateException("无法写入 Download 文件");
                output.write(lastJson.getBytes(StandardCharsets.UTF_8));
            }
            ContentValues ready = new ContentValues();
            ready.put(MediaStore.Downloads.IS_PENDING, 0);
            getContentResolver().update(uri, ready, null, null);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/json");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "分享课程 JSON"));
            status.setText("JSON 已保存到 Download/蹭课网，并已打开系统分享面板。");
        } catch (Exception error) {
            if (uri != null) getContentResolver().delete(uri, null, null);
            status.setText("分享失败：" + error.getMessage());
        }
    }

    private String readAsset(String name) {
        try (InputStream input = getAssets().open(name)) {
            byte[] bytes = new byte[input.available()];
            int offset = 0, read;
            while (offset < bytes.length && (read = input.read(bytes, offset, bytes.length - offset)) > 0) offset += read;
            return new String(bytes, 0, offset, StandardCharsets.UTF_8);
        } catch (Exception error) { return "throw new Error(" + org.json.JSONObject.quote("无法读取资源 " + name) + ");"; }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
