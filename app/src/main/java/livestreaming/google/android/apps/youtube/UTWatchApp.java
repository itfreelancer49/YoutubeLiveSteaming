package livestreaming.google.android.apps.youtube;

import android.app.Application;
import android.os.Environment;

import java.io.File;

import livestreaming.google.android.apps.youtube.util.Utils;

/**
 * Created by anurag on 22/10/17.
 */

public class UTWatchApp extends Application{
    private File utWatchMeFolder;

    @Override
    public void onCreate() {
        super.onCreate();
        File utWatchMeFolder = Utils.createFolder(Environment.getExternalStorageDirectory() +
                "/UTWatchMeVideos");
        Utils.deleteContentsOfDirectory(utWatchMeFolder);
    }

}
