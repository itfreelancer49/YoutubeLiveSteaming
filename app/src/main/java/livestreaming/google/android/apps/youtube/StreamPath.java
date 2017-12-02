package livestreaming.google.android.apps.youtube;

/**
 * Created by Noman Khan on 23/09/17.
 */

public class StreamPath {

    private String videoPath;
    private boolean isStreamed;

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public boolean isStreamed() {
        return isStreamed;
    }

    public void setStreamed(boolean streamed) {
        isStreamed = streamed;
    }
}
