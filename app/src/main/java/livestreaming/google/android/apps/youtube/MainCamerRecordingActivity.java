package livestreaming.google.android.apps.youtube;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;
import java.io.IOException;
import java.util.Date;

public class MainCamerRecordingActivity extends AppCompatActivity implements Callback,
        OnClickListener {
    private VideoView videoView = null;

    private SurfaceHolder surfaceHolder;
    private SurfaceView surfaceView;
    public MediaRecorder mediaRecorder = new MediaRecorder();
    private Camera mCamera;

    private Button btnStart;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen_recording);

        surfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        mCamera = Camera.open();
        // mCamera.setDisplayOrientation(90);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStart.setOnClickListener(this);
    }

    @SuppressLint("NewApi")
    protected void startRecording() throws IOException {
        if (mCamera == null)
            mCamera = Camera.open();

        File sdCard = Environment.getExternalStorageDirectory();
        File dir = new File(sdCard.getAbsolutePath() + "/WWF/WWF Videos");
        if (!dir.exists()) {
            dir.mkdirs();
        }

        Date date = new Date();
        String fileName =  "/rec" + date.toString().replace(" ", "_").replace(":", "_") + ".mp4";
        File file = new File(dir, fileName);

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
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
            mediaRecorder.setOutputFile(dir + fileName);
            mediaRecorder.setOrientationHint(90);
            mediaRecorder.prepare();
            mediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshGallery(file);
    }

    private void refreshGallery(File file) {
        Intent mediaScanIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(Uri.fromFile(file));
        sendBroadcast(mediaScanIntent);
    }

    protected void stopRecording() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mCamera.release();
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

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mCamera != null) {
            Parameters params = mCamera.getParameters();
            mCamera.setParameters(params);
            mCamera.setDisplayOrientation(90);
            Log.i("Surface", "Created");
        } else {
            Toast.makeText(getApplicationContext(), "Camera not available!",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mCamera.stopPreview();
        mCamera.release();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                if (btnStart.getText().toString().equalsIgnoreCase("Start")) {
                    btnStart.setText("Stop");
                    try {
                        startRecording();
                    } catch (IOException e) {
                        String message = e.getMessage();
                        Log.i(null, "Problem " + message);
                        mediaRecorder.release();
                        e.printStackTrace();
                    }
                } else {
                    btnStart.setText("Start");
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                }
                break;

            default:
                break;
        }
    }
}

