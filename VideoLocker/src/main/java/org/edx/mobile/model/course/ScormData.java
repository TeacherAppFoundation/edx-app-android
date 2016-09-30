package org.edx.mobile.model.course;

import com.google.gson.annotations.SerializedName;

import org.edx.mobile.model.api.TranscriptModel;

/**
 * Created by Dimon_GDA on 9/22/16.
 */

public class ScormData extends BlockData {



    @SerializedName("last_modified")
    public String lastModified;

    @SerializedName("scorm_data")
    public String scormData;

}
