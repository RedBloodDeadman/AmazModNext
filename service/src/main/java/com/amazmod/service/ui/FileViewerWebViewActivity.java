package com.amazmod.service.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableFrameLayout;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.amazmod.service.R;
import com.amazmod.service.util.ButtonListener;
import com.amazmod.service.util.SystemProperties;


public class FileViewerWebViewActivity extends Activity {

    BoxInsetLayout boxInsetLayout;
    WearableFrameLayout frameLayoutImage, frameLayoutText;
    WebView webviewImage, webviewText;

    public static final String FILE_URI = "fileUri";
    public static final String MIME_TYPE = "mimeType";
    public static final String IMAGE = "image";

    private ButtonListener buttonListener = new ButtonListener();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer_webview);

        String fileUri = getIntent().getStringExtra(FILE_URI);
        String mimeType = getIntent().getStringExtra(MIME_TYPE);

        boxInsetLayout = findViewById(R.id.activity_file_viewer_main_layout);
        frameLayoutImage = findViewById(R.id.activity_file_viewer_frame_layout_image);
        webviewImage = findViewById(R.id.activity_file_viewer_webview_image);
        frameLayoutText = findViewById(R.id.activity_file_viewer_frame_layout_text);
        webviewText = findViewById(R.id.activity_file_viewer_webview_text);

        if (mimeType.contains(IMAGE)) {
            frameLayoutText.setVisibility(View.GONE);
            loadWebView(fileUri, webviewImage, true);

        } else {
            frameLayoutImage.setVisibility(View.GONE);
            loadWebView(fileUri, webviewText, false);
        }
        setupBtnListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        buttonListener.stop();
    }

    private void loadWebView(String fileUri, WebView webView, boolean isImage) {

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(false);
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);

        if (isImage) {
            webSettings.setLoadWithOverviewMode(true);
            webSettings.setUseWideViewPort(true);
        }

        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(fileUri);

    }


    private static class WebViewClient extends android.webkit.WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

    boolean isMainViewShow = false;

    @Override
    public void onPause() {
        super.onPause();
        isMainViewShow = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        isMainViewShow = true;
    }

    private void setupBtnListener() {
        Activity activity = this;
        buttonListener.start(activity, keyEvent -> {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (isMainViewShow && SystemProperties.isStratos3())
                        switch (keyEvent.getCode()) {
                            case ButtonListener.S3_KEY_MIDDLE_UP:
                                if (webviewText.getScrollY() > 0)
                                    webviewText.scrollTo(webviewText.getScrollX(), webviewText.getScrollY() - 50);
                                if (webviewImage.getScrollY() > 0)
                                    webviewImage.scrollTo(webviewImage.getScrollX(), webviewImage.getScrollY() - 50);
                                break;
                            case ButtonListener.S3_KEY_MIDDLE_DOWN:
                                webviewText.scrollTo(webviewText.getScrollX(), webviewText.getScrollY() + 50);
                                webviewImage.scrollTo(webviewImage.getScrollX(), webviewImage.getScrollY() + 50);
                                break;
                        }
                }
            });
        });
    }
}
