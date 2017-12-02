/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package livestreaming.google.android.apps.youtube;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;
import android.widget.ToggleButton;


import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import livestreaming.google.android.apps.youtube.util.Utils;
import livestreaming.google.android.apps.youtube.util.YouTubeApi;

/**
 * @author Ibrahim Ulukaya <ulukaya@google.com> <p/>// StreamerActivity class which previews the
 *         camera and streams via StreamerService.
 */
public class StreamerActivity extends Activity implements SurfaceHolder.Callback {

    // CONSTANTS
    // TODO: Stop hardcoding this and read values from the camera's supported sizes.
    public static final int CAMERA_WIDTH = 640;
    public static final int CAMERA_HEIGHT = 480;
    private static final int REQUEST_CAMERA_MICROPHONE = 0;
    private static final int REQUEST_CAMERA_MROPHONE = 0;

    // Member variables
    private PowerManager.WakeLock wakeLock;
    private Preview preview;
    private String rtmpUrl;


    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;
    public MediaRecorder mediaRecorder = new MediaRecorder();
    private Camera mCamera;

    private FFmpeg ffmpeg;

    private String broadcastId;

    private int previewHeight = 0;

    private int previewWidth = 0;

    private int previewFormat = 0;
    private Rect r;
    private byte[] mCallbackBuffer;
    private String path;
    private ToggleButton toggleButton;
    private List<StreamPath> listPath;
    private int streamIndex = 0;
    private String pathStreamPath;
    private Handler handler;
    private Runnable runnable;

    private ProgressDialog mProgressDialog;

    private boolean isStreamingStarted = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(MainActivity.APP_NAME, "onCreate");
        super.onCreate(savedInstanceState);

        listPath = new ArrayList<>();

        ffmpeg = FFmpeg.getInstance(this);


        try {
            //Load the binary
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {
                }

                @Override
                public void onFailure() {
                }

                @Override
                public void onSuccess() {
                }

                @Override
                public void onFinish() {
                }
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }

        broadcastId = getIntent().getStringExtra(YouTubeApi.BROADCAST_ID_KEY);
        //Log.v(MainActivity.APP_NAME, broadcastId);

        rtmpUrl = getIntent().getStringExtra(YouTubeApi.RTMP_URL_KEY);

        if (rtmpUrl == null) {
            Log.w(MainActivity.APP_NAME, "No RTMP URL was passed in; bailing.");
            finish();
        }
        Log.i(MainActivity.APP_NAME,
                String.format("Got RTMP URL '%s' from calling activity.", rtmpUrl));

        setContentView(R.layout.streamer);

        surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        toggleButton = (ToggleButton) findViewById(R.id.toggleBroadcasting);

    }


   /* @Override
    protected void onResume() {
        Log.d(MainActivity.APP_NAME, "onResume");

        super.onResume();

        if (streamerService != null) {
            restoreStateFromService();
        }
    }

    @Override
    protected void onPause() {
        Log.d(MainActivity.APP_NAME, "onPause");

        super.onPause();

        if (preview != null) {
            preview.setCamera(null);
        }

        if (streamerService != null) {
            streamerService.releaseCamera();
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(MainActivity.APP_NAME, "onDestroy");

        super.onDestroy();

        if (streamerConnection != null) {
            unbindService(streamerConnection);
        }

        stopStreaming();

        if (streamerService != null) {
            streamerService.releaseCamera();
        }
    }

    private void restoreStateFromService() {
        preview.setCamera(Utils.getCamera(Camera.CameraInfo.CAMERA_FACING_FRONT));
    }*/

/*
    private void startStreaming() {
        Log.d(MainActivity.APP_NAME, "startStreaming");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this.getClass().getName());
        wakeLock.acquire();

        if (!streamerService.isStreaming()) {

            String cameraPermission = Manifest.permission.CAMERA;
            String microphonePermission = Manifest.permission.RECORD_AUDIO;
            int hasCamPermission = checkSelfPermission(cameraPermission);
            int hasMicPermission = checkSelfPermission(microphonePermission);
            List<String> permissions = new ArrayList<String>();
            if (hasCamPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(cameraPermission);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    // Provide rationale in Snackbar to request permission
                    Snackbar.make(preview, R.string.permission_camera_rationale,
                            Snackbar.LENGTH_INDEFINITE).show();
                } else {
                    // Explain in Snackbar to turn on permission in settings
                    Snackbar.make(preview, R.string.permission_camera_explain,
                            Snackbar.LENGTH_INDEFINITE).show();
                }
            }
            if (hasMicPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(microphonePermission);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)) {
                    // Provide rationale in Snackbar to request permission
                    Snackbar.make(preview, R.string.permission_microphone_rationale,
                            Snackbar.LENGTH_INDEFINITE).show();
                } else {
                    // Explain in Snackbar to turn on permission in settings
                    Snackbar.make(preview, R.string.permission_microphone_explain,
                            Snackbar.LENGTH_INDEFINITE).show();
                }
            }
            if (!permissions.isEmpty()) {
                String[] params = permissions.toArray(new String[permissions.size()]);
                ActivityCompat.requestPermissions(this, params, REQUEST_CAMERA_MICROPHONE);
            } else {
                // We already have permission, so handle pathMedia normal
                streamerService.startStreaming(rtmpUrl, preview);
            }
        }
    }
*/

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_MICROPHONE: {
                Log.i(MainActivity.APP_NAME,
                        "Received response for camera with mic permissions request.");

                // We have requested multiple permissions for contacts, so all of them need to be
                // checked.
                if (Utils.verifyPermissions(grantResults)) {
                    // permissions were granted, yay! do the
                    // streamer task you need to do.
                    // streamerService.startStreaming(rtmpUrl, preview);
                } else {
                    Log.i(MainActivity.APP_NAME, "Camera with mic permissions were NOT granted.");
                    Snackbar.make(preview, R.string.permissions_not_granted,
                            Snackbar.LENGTH_SHORT)
                            .show();
                }
                break;
            }

            // other 'switch' lines to check for other
            // permissions this app might request
        }
        return;
    }


    /*private void stopStreaming() {
        Log.d(MainActivity.APP_NAME, "stopStreaming");

        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }

        if (streamerService.isStreaming()) {
            streamerService.stopStreaming();
        }
    }*/

    public void endEvent(View view) {
        stopRecording();

        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(StreamerActivity.this, null,
                    getResources().getText(R.string.endingEvent), true);
            mProgressDialog.setCancelable(false);
        }

        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }

        if (handler != null) {
            handler.removeCallbacks(runnable);
        }

        if (!isStreamingStarted) {
            if (streamIndex == 0) {
                streamInit(rtmpUrl, getPathStreamPath());
            }
        }


    }

    private void finishCurrentActivity() {
        Intent data = new Intent();
        data.putExtra(YouTubeApi.BROADCAST_ID_KEY, broadcastId);
        if (getParent() == null) {
            setResult(Activity.RESULT_OK, data);
        } else {
            getParent().setResult(Activity.RESULT_OK, data);
        }
        finish();
    }

    @SuppressLint("NewApi")
    protected void startRecording(String path) throws IOException {
        if (mCamera == null) {
            mCamera = Camera.open();
        }

        try {
            mediaRecorder = new MediaRecorder();
            mCamera.lock();
            mCamera.unlock();
            // Please maintain sequence of following code.
            // If you change sequence it will not work.
            mediaRecorder.setCamera(mCamera);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
           /* int width = 320;
            int height = 240;
            try {
                //get the available sizes of the video
                List<Camera.Size> tmpList = getSupportedVideoSizes();

                final List<Camera.Size> sizeList = new Vector<Camera.Size>();

                // compare the apsect ratio of the candidate sizes against the
                // real ratio
                Double aspectRatio = (Double.valueOf(getWindowManager()
                        .getDefaultDisplay().getHeight()) / getWindowManager()
                        .getDefaultDisplay().getWidth());
                for (int i = tmpList.size() - 1; i > 0; i--) {
                    Double tmpRatio = Double.valueOf(tmpList.get(i).height)
                            / tmpList.get(i).width;

                    if (Math.abs(aspectRatio - tmpRatio) < .15) {
                        width = tmpList.get(i).width;
                        height = tmpList.get(i).height;
                        sizeList.add(tmpList.get(i));
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
*/

            // set the size of video.
            // If the size is not applicable then throw the media recorder stop
            // -19 error
            mediaRecorder.setVideoSize(720, 480);


            // Set the video encoding bit rate this changes for the high, low.
            // medium quality devices
            mediaRecorder.setVideoEncodingBitRate(1700000);

            //Set the video frame rate
            mediaRecorder.setVideoFrameRate(30);

            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            mediaRecorder.setOutputFile(path);
            mediaRecorder.setOrientationHint(90);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Camera.Size> getSupportedVideoSizes() {
        Parameters params = mCamera.getParameters();
        if (params.getSupportedVideoSizes() != null) {
            return params.getSupportedVideoSizes();
        } else {
            // Video sizes may be null, which indicates that all the supported
            // preview sizes are supported for video recording.
            return params.getSupportedPreviewSizes();
        }
    }


    protected void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release();
            // mCamera.release();
            // mCamera.lock();
        }
    }

    private void releaseMediaRecorder() {

        if (mediaRecorder != null) {
            mediaRecorder.reset(); // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release(); // release the camera for other applications
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

        holder.setFixedSize(width, height);
        // Start the preview
        Parameters params = mCamera.getParameters();
        previewHeight = params.getPreviewSize().height;
        previewWidth = params.getPreviewSize().width;
        previewFormat = params.getPreviewFormat();

        // Crop the edges of the picture to reduce the image size
        r = new Rect(80, 20, previewWidth, previewHeight);

        mCallbackBuffer = new byte[460800];
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }
        mCamera.setParameters(params);
        mCamera.setDisplayOrientation(90);

        try {
            mCamera.setPreviewDisplay(surfaceHolder);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // mCamera.setPreviewCallback(Camera.this);
        mCamera.startPreview();

        try {
            startRecording(getPath());
        } catch (IOException e) {
            e.printStackTrace();
        }


        handler = new Handler();

        runnable = new Runnable() {
            public void run() {
                try {
                    // Do something after 5s = 5000ms
                    MainActivity.test();
                    stopRecording();
                    startRecording(getPath());

                    // toggleButton.setEnabled(false);
                    if (streamIndex == 0) {
                        streamInit(rtmpUrl, getPathStreamPath());
                        ++streamIndex;
                    }
                    handler.postDelayed(runnable, 10000);
                    Log.e("calling", "handler");
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        };

        handler.postDelayed(runnable, 10000);

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            // check for exceptions
            System.err.println(e);
            e.printStackTrace();
            return;
        }


        if (mCamera != null) {
            Parameters params = mCamera.getParameters();
            List<String> focusModes = params.getSupportedFocusModes();
            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(90);

            try {
                mCamera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mCamera.startPreview();
            Log.i("Surface", "Created");
        } else {
            Toast.makeText(getApplicationContext(), "Camera not available!",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        try {
            mCamera.stopPreview();
            mCamera.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void streamInit(String url, final String path) {
        try {
            isStreamingStarted = true;
            String cmd = "-re -i " + path + " -c copy -f flv "
                    + url;

            ffmpeg.execute(cmd,
                    new ExecuteBinaryResponseHandler() {

                        @Override
                        public void onStart() {
                            //for logcat
                            Log.e("start", "Streaming initialized");
                        }

                        @Override
                        public void onProgress(String message) {
                            //for logcat
                            Log.e("progress", message.toString());
                        }

                        @Override
                        public void onFailure(String message) {

                            Log.e("failure", message.toString());
                        }

                        @Override
                        public void onSuccess(String message) {

                            Log.e("success", message.toString());
                        }

                        @Override
                        public void onFinish() {
                            if (getPathStreamPath(streamIndex - 1) != null) {
                                new File(getPathStreamPath(streamIndex - 1)).delete();
                            }
                            //int index = streamIndex + 1;
                            if (listPath.size() > streamIndex) {
                                streamInit(rtmpUrl, getPathStreamPath());
                                // new File(listPath.get(streamIndex).getVideoPath()).delete();
                                ++streamIndex;
                            } else {
                                if (mProgressDialog != null && mProgressDialog.isShowing()) {
                                    mProgressDialog.dismiss();
                                }
                                finishCurrentActivity();
                            }
                            Log.d(null, "Streaming done..");
                        }
                    });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            e.printStackTrace();
            Log.w(null, e.toString());
        }
    }

    public String getPathStreamPath() {
        StreamPath streamPath = listPath.get(streamIndex);
        return streamPath.getVideoPath();
    }

    public String getPathStreamPath(int index) {
        StreamPath streamPath = listPath.get(index);
        return streamPath.getVideoPath();
    }

    public void setPathStreamPath(String pathStreamPath) {
        this.pathStreamPath = pathStreamPath;
    }


    private String getPath() {
        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/UTWatchMeVideos");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Date date = new Date();
        String fileName = "/rec" + date.toString().replace(" ", "_").replace(":", "_") + ".mp4";
        File file = new File(dir, fileName);
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String absolutePath = file.getAbsolutePath();

        StreamPath stPath = new StreamPath();
        stPath.setVideoPath(absolutePath);
        stPath.setStreamed(false);
        listPath.add(stPath);

        return absolutePath;
    }
}
