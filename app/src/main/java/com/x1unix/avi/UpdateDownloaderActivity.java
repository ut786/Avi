package com.x1unix.avi;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.x1unix.avi.helpers.DownloadFileFromURL;
import com.x1unix.avi.model.AviSemVersion;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;

public class UpdateDownloaderActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private WebView webView;

    private TextView txUpdateProgress;
    private TextView txUpdateTag;
    private AviSemVersion updatePkg;

    private TextView txUpdateStatus;
    private Resources res;

    private final String APK_NAME = "avi.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_downloader);

        final Intent intent = getIntent();
        res = getResources();

        if (intent != null) {
            updatePkg = (AviSemVersion) intent.getSerializableExtra("update");
            if (updatePkg != null) {
                initView();
                loadUpdateInformation();
            }
        }
    }

    private void initView() {
        txUpdateProgress = (TextView) findViewById(R.id.avi_update_progress);
        txUpdateTag = (TextView) findViewById(R.id.avi_update_tag);
        txUpdateStatus = (TextView) findViewById(R.id.avi_update_status);

        initProgressBar();
        initWebView();

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                downloadPackage();
            }
        }, 2000);
    }

    private void setDownloadProgress(int progress) {
        progressBar.setProgress(progress);
        txUpdateProgress.setText(String.valueOf(progress) + "%");
    }

    private void downloadPackage() {
        boolean deleted = true;
        File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + APK_NAME);
        if (f.exists()) {
            deleted = f.delete();
        }

        f = null;

        // Fire UP download!
        AsyncTask<String, String, String> task = new DownloadFileFromURL() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                txUpdateStatus.setText(res.getString(R.string.downloading_update));
                progressBar.setIndeterminate(false);
                setDownloadProgress(0);
            }

            protected void onProgressUpdate(String... progress) {
                setDownloadProgress(Integer.parseInt(progress[0]));
            }

            @Override
            protected void onPostExecute(String file_url) {
                if (failed) {
                    panic(error);
                } else {
                    progressBar.setVisibility(View.GONE);
                    txUpdateProgress.setText("");
                    initInstallPackage();
                }
            }
        };

        try {
            task.execute(updatePkg.getApkUrl(), APK_NAME);
        } catch (Exception ex) {
            panic(ex.getMessage());
        }
    }

    private void initInstallPackage() {
        txUpdateStatus.setText(res.getString(R.string.installing_package));
        Intent intent = new Intent(Intent.ACTION_VIEW);

        final String apkDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + APK_NAME;
        intent.setDataAndType(Uri.fromFile(new File(apkDir)), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void panic(String err) {
        AlertDialog.Builder dial = new AlertDialog.Builder(this);
        dial.setTitle(res.getString(R.string.error))
                .setMessage(res.getString(R.string.failed_to_download_update) + err)
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        finish();
                    }
                }).show();
    }

    private void loadUpdateInformation() {

        txUpdateTag.setText(updatePkg.getTag());

        if (updatePkg.hasChangelog()) {
            webView.loadData(getDecoratedChangelogHTML(updatePkg.getChangelog()), "text/html; charset=UTF-8", null);
        }
    }

    private String getDecoratedChangelogHTML(String changelogHTML) {
        String result = "";
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("changelog_template.html"), "UTF-8"));

            // do reading, usually loop until end of file reading
            String mLine;
            String mTemplate = "";
            while ((mLine = reader.readLine()) != null) {
                mTemplate += mLine;
            }

            result = mTemplate.replaceAll("%CONTENT%", changelogHTML);

        } catch (IOException e) {
            //log the exception
            result = changelogHTML;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    result = changelogHTML;
                }
            }
        }
        return result;
    }

    private void initProgressBar() {
        progressBar = (ProgressBar) findViewById(R.id.avi_update_bar);
        progressBar.getIndeterminateDrawable().setColorFilter(
                getResources().getColor(R.color.colorAccentDark),
                android.graphics.PorterDuff.Mode.SRC_IN);
        progressBar.getProgressDrawable().setColorFilter(
                getResources().getColor(R.color.colorAccentDark),
                android.graphics.PorterDuff.Mode.SRC_IN);

        progressBar.setVisibility(View.VISIBLE);
    }

    private void initWebView() {
        webView = (WebView) findViewById(R.id.avi_update_changelog);
        webView.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDefault));
        webView.setVisibility(View.VISIBLE);
    }
}
