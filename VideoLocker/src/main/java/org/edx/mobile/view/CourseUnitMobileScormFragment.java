package org.edx.mobile.view;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.inject.Inject;

import org.edx.mobile.R;
import org.edx.mobile.course.ScormManager;
import org.edx.mobile.model.course.BlockType;
import org.edx.mobile.model.course.CourseComponent;
import org.edx.mobile.model.course.ScormBlockModel;
import org.edx.mobile.services.ViewPagerDownloadManager;
import org.edx.mobile.util.BrowserUtil;

/**
 *
 */
public class CourseUnitMobileScormFragment extends CourseUnitFragment {

    @Inject
    @NonNull
    ScormManager scormManager;

    private Button showButton;
    private Button downloadButton;
    private Button deleteButton;
    private ProgressDialog progressDialog;
    private BroadcastReceiver scormReceiver;
    private TextView deleteUnitLabelTextView,unitLabelTextView;
    private String path;
    boolean cancel = false;

    /**
     * Create a new instance of fragment
     */
    public static CourseUnitMobileScormFragment newInstance(CourseComponent unit) {
        CourseUnitMobileScormFragment f = new CourseUnitMobileScormFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putSerializable(Router.EXTRA_COURSE_UNIT, unit);
        f.setArguments(args);

        return f;
    }

    /**
     * When creating, retrieve this instance's number from its arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * The Fragment's UI is just a simple text view showing its
     * instance number.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_course_unit_scorm, container, false);
//        ((TextView) v.findViewById(R.id.not_available_message)).setText(
//                unit.getType() == BlockType.VIDEO ? R.string.video_only_on_web_short : R.string.assessment_not_available);
        downloadButton = (Button) v.findViewById(R.id.download_button);
        showButton = (Button) v.findViewById(R.id.show_button);
        deleteButton = (Button) v.findViewById(R.id.delete_button);
        deleteUnitLabelTextView = (TextView) v.findViewById(R.id.delete_unit_label);
        unitLabelTextView = (TextView) v.findViewById(R.id.unit_label);

        updateUI();

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDownload();
            }

        });

        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doShow(scormManager.get(unit.getId()));
            }

        });


        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                path = scormManager.get(unit.getId());
                scormManager.deleteUnit(path);
                updateUI();
            }

        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (ViewPagerDownloadManager.instance.inInitialPhase(unit))
            ViewPagerDownloadManager.instance.addTask(this);
    }

    @Override
    public void onResume() {
        scormReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("Progress");
                Log.d("LocalBroadcast", "Progress : " + (Integer.parseInt(message)));
                if (progressDialog != null) {
                    progressDialog.setProgress(Integer.parseInt(message));
                }
            }
        };

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(scormReceiver, new IntentFilter("org.edx.mobile.scorm"));
        super.onResume();
    }

    @Override
    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(scormReceiver);
        super.onPause();
    }

    private void doShow(String folder) {

        startActivity(CourseScormViewActivity.getLaunchIntent(getActivity(), folder));
    }


    public void showWaitingDialog() {
//        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
//        }

        progressDialog.setMessage("Downloading file. Please wait...");
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                progressDialog.setCancelable(true);
                cancel = true;
                Intent intent = new Intent("org.edx.mobile.scormCancel");
                LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(intent);
                dialog.dismiss();
            }
        });
        if (progressDialog != null && !progressDialog.isShowing() && !getActivity().isFinishing()) {
            try {
                progressDialog.show();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void dismissWaitingDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void doDownload() {

        showWaitingDialog();
        scormManager.startScormDownload((ScormBlockModel) unit, new ScormManager.DownloadListener() {
            @Override
            public void handle(final Exception ex) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message;
                        dismissWaitingDialog();
                        message = "Download error";

                        if (ex != null) {
                            message = ex.getLocalizedMessage();
                        }
                        if (cancel) {
                            message = "Cancel download";
                            cancel = false;
                        }
                        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onDownloadComplete(String response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissWaitingDialog();
                        updateUI();
                    }
                });

            }
        });
    }

    @Override
    public void run() {
        ViewPagerDownloadManager.instance.done(this, true);
    }

    private void updateUI() {
        downloadButton.setVisibility(scormManager.has(unit.getId()) ? View.GONE : View.VISIBLE);
        showButton.setVisibility(scormManager.has(unit.getId()) ? View.VISIBLE : View.GONE);
        deleteButton.setVisibility(scormManager.has(unit.getId()) ? View.VISIBLE : View.GONE);
        deleteUnitLabelTextView.setVisibility(scormManager.has(unit.getId()) ? View.VISIBLE : View.GONE);

        if(scormManager.has(unit.getId())) {
           unitLabelTextView.setText("This unit is available for offline view");
        } else {
            unitLabelTextView.setText("Download to see this unit offline");
        }
    }
}
