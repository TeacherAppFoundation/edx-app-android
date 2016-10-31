package org.edx.mobile.course;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;

import org.edx.mobile.R;
import org.edx.mobile.logger.Logger;
import org.edx.mobile.model.api.TranscriptModel;
import org.edx.mobile.model.course.ScormBlockModel;
import org.edx.mobile.util.IOUtils;
import org.edx.mobile.util.ScormDownloader;
import org.edx.mobile.util.Sha1Util;
import org.edx.mobile.util.TranscriptDownloader;

@Singleton
public class ScormManager {

    private File scormFolder;
    private Context context;
    private final Logger logger = new Logger(getClass().getName());

    @Inject
    public ScormManager(Context context) {
        try {
            this.context = context;
            File android = new File(Environment.getExternalStorageDirectory(), "Android");
            File downloadsDir = new File(android, "data");
            File packDir = new File(downloadsDir, context.getPackageName());
            scormFolder = new File(packDir, "scormFolder");
            if (!scormFolder.exists()) {
                scormFolder.mkdirs();
            }
        } catch (Exception e) {
            logger.error(e);
        }
    }

    /**
     * This function checks if the file exists for that link
     *
     * @param url
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public boolean has(String url) {
        String hash;
        try {
            hash = Sha1Util.SHA1(url);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }
        File file = new File(scormFolder, hash);
        return file.exists() && file.isDirectory();
    }

    public void deleteUnit(String path) {

        boolean deleted = deleteRecursive(new File(path));
    }

    public boolean deleteRecursive(File fileOrDirectory) {

        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteRecursive(child);
            }
        }

        return fileOrDirectory.delete();
    }

    public String get(String url) {
        String hash;
        try {
            hash = Sha1Util.SHA1(url);
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
        File file = new File(scormFolder, hash);
        return file.getAbsolutePath();
    }


    /**
     * This function is used to saved contents of a String to a file
     * @param url - Url of Transcript
     * @param response - This is the String which needs to be saved into a file
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
//    public void put(String url, String response)
//            throws NoSuchAlgorithmException, UnsupportedEncodingException,
//            IOException {
//        String hash = Sha1Util.SHA1(url);
//        File file = new File(scormFolder, hash);
//        FileOutputStream out = new FileOutputStream(file);
//        out.write(response.getBytes());
//        out.close();
//    }


    /**
     * This function helps to get the file contents in a String
     * @param url - This is the URL for SRT files
     * @return String - This is the response of the File contents
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
//    public String get(String url) throws IOException, NoSuchAlgorithmException {
//        try{
//            String hash = Sha1Util.SHA1(url);
//            File file = new File(scormFolder, hash);
//            if (!file.exists()) {
//                // not in cache
//                return null;
//            }
//
//            FileInputStream in = new FileInputStream(file);
//            String cache = IOUtils.toString(in, Charset.defaultCharset());
//            in.close();
//            logger.debug("Cache.get=" + hash);
//            return cache;
//        }catch(Exception e){
//            logger.error(e);
//        }
//        return null;
//    }


    /**
     * This function helps to get the file contents in an InputStream
     * @param url - This is the URL for SRT files
     * @return String - This is the response of the File contents
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
//    public InputStream getInputStream(String url) throws IOException, NoSuchAlgorithmException {
//        try{
//            String hash = Sha1Util.SHA1(url);
//            File file = new File(scormFolder, hash);
//            if (!file.exists()) {
//                // not in cache
//                return null;
//            }
//
//            InputStream in = new FileInputStream(file);
//            return in;
//        }catch(Exception e){
//            logger.error(e);
//        }
//        return null;
//    }


    /**
     * This function is used to handle downloading of SRT files and saving them
     *
     * @param download
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public void startScormDownload(final ScormBlockModel download, final DownloadListener downloadListener) {
        //Uri target = Uri.fromFile(new File(transcriptFolder, Sha1Util.SHA1(downloadLink)));
        if (download == null || download.getData() == null || TextUtils.isEmpty(download.getData().scormData)) {

            downloadListener.handle(null);
            return;
        }

        String hash;
        try {
            hash = Sha1Util.SHA1(download.getId());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            e.printStackTrace();
            downloadListener.handle(e);
            return;
        }
        File file = new File(scormFolder, hash);


        //If file is not present in the Folder, then start downloading
        if (!has(download.getId())) {

            ScormDownloader td = new ScormDownloader(context, download.getData().scormData, file.getAbsolutePath()) {

                @Override
                public void onDownloadComplete(String response) {
                    downloadListener.onDownloadComplete(response);
                }

                @Override
                public void handle(Exception ex) {
                    logger.error(ex);
                    downloadListener.handle(ex);
                }
            };
            Thread th = new Thread(td);
            th.start();
        } else {
            downloadListener.onDownloadComplete(file.getAbsolutePath());
        }
    }

    /**
     * This function starts downloading all the srt files in a Transcript model
     * @param transcript
     */
//    public void downloadTranscriptsForVideo(TranscriptModel transcript){
//        if(transcript==null){
//            return;
//        }
//
//        if(transcript.chineseUrl!=null){
//            try {
//                startTranscriptDownload(transcript.chineseUrl);
//            } catch (NoSuchAlgorithmException e) {
//                logger.error(e);
//            } catch (UnsupportedEncodingException e) {
//                logger.error(e);
//            }
//        }
//        if(transcript.englishUrl!=null){
//            try {
//                startTranscriptDownload(transcript.englishUrl);
//            } catch (NoSuchAlgorithmException e) {
//                logger.error(e);
//            } catch (UnsupportedEncodingException e) {
//                logger.error(e);
//            }
//        }
//        if(transcript.frenchUrl!=null){
//            try {
//                startTranscriptDownload(transcript.frenchUrl);
//            } catch (NoSuchAlgorithmException e) {
//                logger.error(e);
//            } catch (UnsupportedEncodingException e) {
//                logger.error(e);
//            }
//        }
//        if(transcript.germanUrl!=null){
//            try {
//                startTranscriptDownload(transcript.germanUrl);
//            } catch (NoSuchAlgorithmException e) {
//                logger.error(e);
//            } catch (UnsupportedEncodingException e) {
//                logger.error(e);
//            }
//        }
//        if(transcript.portugueseUrl!=null){
//            try {
//                startTranscriptDownload(transcript.portugueseUrl);
//            } catch (NoSuchAlgorithmException e) {
//                logger.error(e);
//            } catch (UnsupportedEncodingException e) {
//                logger.error(e);
//            }
//        }
//        if(transcript.spanishUrl!=null){
//            try {
//                startTranscriptDownload(transcript.spanishUrl);
//            } catch (NoSuchAlgorithmException e) {
//                logger.error(e);
//            } catch (UnsupportedEncodingException e) {
//                logger.error(e);
//            }
//        }
//    }


    /**
     * This function is used to fetch all language Transcripts of a particular Video in strings
     * @param transcript - This model contains links of the srt files
     * @return ArrayList<String> which is the list of srt response strings
     */
//    public LinkedHashMap<String, InputStream> fetchTranscriptsForVideo(
//                TranscriptModel transcript, Context context){
//
//        LinkedHashMap<String, InputStream> transcriptList = new LinkedHashMap<String, InputStream>();
//        try{
//            if(transcript.chineseUrl!=null){
//                transcriptList.put(context.getString(R.string.cc_chinese_code),
//                        fetchTranscriptResponse(transcript.chineseUrl));
//            }
//
//            if(transcript.englishUrl!=null){
//                transcriptList.put(context.getString(R.string.cc_english_code),
//                        fetchTranscriptResponse(transcript.englishUrl));
//            }
//
//            if(transcript.frenchUrl!=null){
//                transcriptList.put(context.getString(R.string.cc_french_code),
//                        fetchTranscriptResponse(transcript.frenchUrl));
//            }
//
//            if(transcript.germanUrl!=null){
//                transcriptList.put(context.getString(R.string.cc_german_code),
//                        fetchTranscriptResponse(transcript.germanUrl));
//            }
//
//            if(transcript.portugueseUrl!=null){
//                transcriptList.put(context.getString(R.string.cc_portugal_code),
//                        fetchTranscriptResponse(transcript.portugueseUrl));
//            }
//
//            if(transcript.spanishUrl!=null){
//                transcriptList.put(context.getString(R.string.cc_spanish_code),
//                        fetchTranscriptResponse(transcript.spanishUrl));
//            }
//
//            return transcriptList;
//        }catch(Exception e){
//            logger.error(e);
//        }
//        return null;
//    }

    /**
     * This function is used to get string as response for the contents of a file
     * //     * @param url - URL of the srt
     *
     * @return String contents of the File
     */
//    public InputStream fetchTranscriptResponse(String url){
//        if(url==null){
//            return null;
//        }
//
//        InputStream response = null;
//        try {
//            if(has(url)){
//                response = getInputStream(url);
//            return response;
//            }
//        } catch (NoSuchAlgorithmException e) {
//            logger.error(e);
//        } catch (UnsupportedEncodingException e) {
//            logger.error(e);
//        } catch (IOException e) {
//            logger.error(e);
//        } catch (Exception e) {
//            logger.error(e);
//        }
//        return null;
//    }


    public interface DownloadListener {

        void handle(Exception ex);

        void onDownloadComplete(String response);
    }
}

