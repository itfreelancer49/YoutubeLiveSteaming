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

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import com.github.hiteshsondhi88.libffmpeg.ExecuteBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.LoadBinaryResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegCommandAlreadyRunningException;
import com.github.hiteshsondhi88.libffmpeg.exceptions.FFmpegNotSupportedException;
import java.io.File;
import java.io.IOException;


public class VideoStreamingConnection implements VideoStreamingInterface {

    // CONSTANTS.
    private static final int AUDIO_SAMPLE_RATE = 44100;

    // Member variables.
    private VideoFrameGrabber videoFrameGrabber;
    private AudioFrameGrabber audioFrameGrabber;
    private Object frame_mutex = new Object();
    private boolean encoding;
    private FFmpeg ffmpeg;
    private MediaRecorder mediaRecorder;

    public VideoStreamingConnection(Context context) {
        ffmpeg = FFmpeg.getInstance(context);

        try {
            //Load the binary
            ffmpeg.loadBinary(new LoadBinaryResponseHandler() {

                @Override
                public void onStart() {}

                @Override
                public void onFailure() {}

                @Override
                public void onSuccess() {}

                @Override
                public void onFinish() {}
            });
        } catch (FFmpegNotSupportedException e) {
            // Handle if FFmpeg is not supported by device
        }

    }

    @Override
    public void open(final String url, Camera camera, Surface previewSurface) {
        Log.d(MainActivity.APP_NAME, "open");

        videoFrameGrabber = new VideoFrameGrabber();
        videoFrameGrabber.setFrameCallback(new VideoFrameGrabber.FrameCallback() {
            @Override
            public void handleFrame(byte[] yuv_image) {
                if (encoding) {
                    synchronized (frame_mutex) {
                        int encoded_size = Ffmpeg.encodeVideoFrame(yuv_image);

                        // Logging.Verbose("Encoded video! Size = " + encoded_size);
                    }
                }
            }
        });

        audioFrameGrabber = new AudioFrameGrabber();
        audioFrameGrabber.setFrameCallback(new AudioFrameGrabber.FrameCallback() {
            @Override
            public void handleFrame(short[] audioData, int length) {
                if (encoding) {
                    synchronized (frame_mutex) {
                        int encoded_size = Ffmpeg.encodeAudioFrame(audioData, length);

                        // Logging.Verbose("Encoded audio! Size = " + encoded_size);
                    }
                }
            }
        });
        File file = new File(Environment.getExternalStorageDirectory() + "/abc.mp4");
        try {
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

       final String path = file.getAbsolutePath();

        // need to work here
        synchronized (frame_mutex) {
            Size previewSize = videoFrameGrabber.start(camera);
            audioFrameGrabber.start(AUDIO_SAMPLE_RATE);


            /*mediaRecorder = new MediaRecorder();

            camera.unlock();
            mediaRecorder.setCamera(camera);

            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
//            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
            mediaRecorder.setOutputFile(path);
            mediaRecorder.setPreviewDisplay(previewSurface);*/
            try {
               /* mediaRecorder.prepare();
                mediaRecorder.start();*/

                mediaRecorder = new MediaRecorder();
                camera.lock();
                camera.unlock();
                // Please maintain sequence of following code.
                // If you change sequence it will not work.
                mediaRecorder.setCamera(camera);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                mediaRecorder.setPreviewDisplay(previewSurface);
                mediaRecorder.setOutputFile(path);
                mediaRecorder.setOrientationHint(90);
                mediaRecorder.prepare();
                mediaRecorder.start();
            } catch (IllegalStateException e) {
                Log.e("Error Recording:", "jj");
            } catch (IOException e) {
                Log.e("Error Recording:", "jj");
            }

            //int width = previewSize.width;
           // int height = previewSize.height;
            //TODO: init FFMPEG
            //encoding = Ffmpeg.init(width, height, AUDIO_SAMPLE_RATE, url);

           // Log.i(MainActivity.APP_NAME, "Ffmpeg.init() returned " + encoding);


          /*  VBR="2500k"
            FPS="30"
            QUAL="medium"
            YOUTUBE_URL="rtmp://a.rtmp.youtube.com/live2"

            SOURCE="udp://239.255.139.0:1234"
            KEY="...."

            ffmpeg \
            -i "$SOURCE" -deinterlace \
            -vcodec libx264 -pix_fmt yuv420p -preset $QUAL -r $FPS -g $(($FPS * 2)) -b:v $VBR \
            -acodec libmp3lame -ar 44100 -threads 6 -qscale 3 -b:a 712000 -bufsize 512k \
            -f flv "$YOUTUBE_URL/$KEY";*/

           // ffmpeg -re -i <input link> -vcodec h264 -ar 44100 -f flv "rtmp://<host>/<publication>"

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // Do something after 5s = 5000ms
                    streamInit(url,  path);
                }
            }, 20000);


        }
    }

    private void streamInit(String url , String path){
        try {
           // url = "rtmp://a.rtmp.youtube.com/live2/g7gq-0a8x-w54q-96fp";

            String VideoIn  = Environment.getExternalStorageDirectory() + "/an.mp4";

            //ffmpeg -re -i /sdcard/sample.mp4 -c copy -f flv rtmp://192.168.1.34:1936/live/myStream

            String cmd = "-re -i " + path + " -c copy -f flv "
                    + url;

           // String cmd =  "ffmpeg -re -i " + VideoIn + " -vcodec h264 -ar 44100 -f flv " + url;
          //  String cmd =  "ffmpeg -re -i " + VideoIn + " -vcodec h264 -ar 44100 -f flv " + url;

            ffmpeg.execute(cmd,
                    new ExecuteBinaryResponseHandler() {

                        @Override
                        public void onStart() {
                            //for logcat
                            Log.w(null,"Cut started");
                        }

                        @Override
                        public void onProgress(String message) {
                            //for logcat
                            Log.w(null,message.toString());
                        }

                        @Override
                        public void onFailure(String message) {

                            Log.w(null,message.toString());
                        }

                        @Override
                        public void onSuccess(String message) {

                            Log.w(null,message.toString());
                        }

                        @Override
                        public void onFinish() {

                            Log.w(null,"Cutting video finished");
                        }
                    });
        } catch (FFmpegCommandAlreadyRunningException e) {
            // Handle if FFmpeg is already running
            e.printStackTrace();
            Log.w(null,e.toString());
        }
    }

    @Override
    public void close() {
        Log.i(MainActivity.APP_NAME, "close");

        mediaRecorder.stop();
        videoFrameGrabber.stop();
        audioFrameGrabber.stop();

        encoding = false;
        if (encoding) {
            Ffmpeg.shutdown();
        }
    }
}
