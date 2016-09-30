package org.edx.mobile.view;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
    private ProgressDialog progressDialog;

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
        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                doDownload();

            }

        });
        downloadButton.setVisibility(scormManager.has(unit.getId()) ? View.GONE : View.VISIBLE);

        showButton = (Button) v.findViewById(R.id.show_button);
        showButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doShow(scormManager.get(unit.getId()));
            }

        });

        showButton.setVisibility(scormManager.has(unit.getId()) ? View.VISIBLE : View.GONE);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (ViewPagerDownloadManager.instance.inInitialPhase(unit))
            ViewPagerDownloadManager.instance.addTask(this);
    }

    private void doShow(String folder) {

        startActivity(CourseScormViewActivity.getLaunchIntent(getActivity(),folder));
    }


    public void showWaitingDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
        }
        progressDialog.setCancelable(false);

        progressDialog.setMessage("");

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
                        dismissWaitingDialog();
                        String message = "Download error";
                        if(ex!=null){
                            message = ex.getLocalizedMessage();
                        }
                        Toast.makeText(getActivity(),message,Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onDownloadComplete(String response) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dismissWaitingDialog();
                        downloadButton.setVisibility(scormManager.has(unit.getId()) ? View.GONE : View.VISIBLE);
                        showButton.setVisibility(scormManager.has(unit.getId()) ? View.VISIBLE : View.GONE);
                    }
                });

            }
        });
    }

    @Override
    public void run() {
        ViewPagerDownloadManager.instance.done(this, true);
    }

}
