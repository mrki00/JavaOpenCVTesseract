import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.highgui.HighGui;
public class Main {
    public static void main(String[] args) {

        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        VideoCapture capture = new VideoCapture();
        capture.open("rtsp://username:password@ipaddress:port/stream");

        if (capture.isOpened()) {
            Mat currentFrame = new Mat();
            Mat previousFrame = new Mat();
            Mat diffFrame = new Mat();

            while (true) {
                if (capture.read(currentFrame)) {
                    if (!previousFrame.empty()) {
                        Core.absdiff(currentFrame, previousFrame, diffFrame);
                        Imgproc.cvtColor(diffFrame, diffFrame, Imgproc.COLOR_BGR2GRAY);
                        Imgproc.threshold(diffFrame, diffFrame, 25, 255, Imgproc.THRESH_BINARY);

                        HighGui.imshow("Frame Difference", diffFrame);
                    }
                    previousFrame = currentFrame.clone();
                } else {
                    System.out.println("Error reading frame from RTSP stream");
                    break;
                }
                if (HighGui.waitKey(1) == 27) {
                    break;
                }
            }
        } else {
            System.out.println("Error opening RTSP stream");
        }
        capture.release();
        HighGui.destroyAllWindows();
    }
}