package org.edx.mobile.view;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.net.URISyntaxException;

import org.edx.mobile.R;

/**
 * Created by Dimon_GDA on 9/23/16.
 */

public class CourseScormViewActivity extends AppCompatActivity {

    private final static String FOLDER_PATH = "FOLDER_PATH";

    private final static String LAUNCHER_FILE = "index.html";


    private WebView mWebView;
    private File launcher;


    public static Intent getLaunchIntent(Context context,String folder) {
        Intent intent = new Intent(context, CourseScormViewActivity.class);
        intent.putExtra(FOLDER_PATH,folder);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scorm_view);

        mWebView = (WebView) findViewById(R.id.webView);

        if (getIntent() != null && getIntent().getExtras() != null) {
            String folderPath = getIntent().getExtras().getString(FOLDER_PATH);

            if(folderPath !=null&& folderPath.length()>0)
                launcher = findFile(new File(folderPath),"",LAUNCHER_FILE);

        }


        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setPluginState(WebSettings.PluginState.ON);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.endsWith("html")) {
                    mWebView.loadUrl(url);
                    return true;
                }

                if (url.startsWith("intent://")) {
                    try {
                        Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

                        if (intent != null) {
                            view.stopLoading();

                            PackageManager packageManager = getPackageManager();
                            ResolveInfo info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                            if (info != null) {
                                startActivity(intent);
                            } else {
                                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                                view.loadUrl(fallbackUrl);

                                Toast.makeText(CourseScormViewActivity.this,"Install player to watch this course",Toast.LENGTH_SHORT).show();

                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + intent.getPackage())));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + intent.getPackage())));
                                }

                                // or call external broswer
//                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl));
//                    context.startActivity(browserIntent);
                            }

                            return true;
                        }
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
                return false;
            }

        });

        if(launcher!=null)
            mWebView.loadUrl("file://"+ launcher.getAbsolutePath());

    }
    private File findFile(File aFile, String sDir, String toFind ){
        if( aFile.isFile() &&
                aFile.getAbsolutePath().contains( sDir ) &&
                aFile.getName().contains( toFind ) ) {
            return aFile;
        } else if( aFile.isDirectory() ) {
            for( File child : aFile.listFiles() ){
                File found = findFile( child, sDir, toFind );
                if( found != null ) {
                    return found;
                }//if
            }//for
        }//else
        return null;
    }
}

