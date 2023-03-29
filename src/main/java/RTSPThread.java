import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import net.sourceforge.tess4j.*;
import java.awt.image.DataBufferByte;
import java.awt.image.BufferedImage;

class RTSPThread extends Thread {
    private String rtspUrl;

    public RTSPThread(String rtspUrl) {
        this.rtspUrl = rtspUrl;
    }

    @Override
    public void run() {
        System.out.println("in run");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.setProperty("jna.library.path", "/opt/homebrew/Cellar/tesseract/5.3.0_1/lib/");
        VideoCapture capture = new VideoCapture();
        capture.open(rtspUrl);
        if (!capture.isOpened()) {
            System.err.println("Failed to open RTSP stream: " + rtspUrl);
            return;
        }
        capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
        capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);

        Mat frame;
        while (true) {
            frame = new Mat();
            capture.read(frame);
            if (frame.empty()) {
                System.err.println("Failed to read frame from RTSP stream: " + rtspUrl);
                try {
                    Thread.sleep(5000);
                    capture.release();
                    capture.open(rtspUrl);
                    continue;
                } catch (InterruptedException e) {
                    capture.release();
                }
            }
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Mat exampleImage = Imgcodecs.imread("/Users/mrki/Downloads/score720.png");
            Mat grayExampleImage = new Mat();
            Imgproc.cvtColor(exampleImage, grayExampleImage, Imgproc.COLOR_BGR2GRAY);
            int x = 460;
            int y = 46;
            int width = grayExampleImage.width();
            int height = grayExampleImage.height();
            Mat searchArea = frame.submat(y, y + height, x, x + width);
            Mat graySearchArea = new Mat();
            Imgproc.cvtColor(searchArea, graySearchArea, Imgproc.COLOR_BGR2GRAY);
            Mat matchResult = new Mat();
            Imgproc.matchTemplate(graySearchArea, grayExampleImage, matchResult, Imgproc.TM_CCOEFF_NORMED);
            double threshold = 0.65;
            Core.MinMaxLocResult mmr = Core.minMaxLoc(matchResult);
            Point matchLoc = mmr.maxLoc;
            if (mmr.maxVal > threshold) {
                System.out.println(mmr.maxVal + " found in " + rtspUrl);
                Point pt1 = new Point(matchLoc.x + x, matchLoc.y + y);
                Point pt2 = new Point(matchLoc.x + x + width, matchLoc.y + y + height);
                Imgproc.rectangle(frame, pt1, pt2, new Scalar(0, 0, 255), 2);

                // Crop the search area to just the region where the image is found
                Mat imageRegion = graySearchArea.submat((int) matchLoc.y, (int) (matchLoc.y + height), (int) matchLoc.x, (int) (matchLoc.x + width));

                // Convert the cropped region to a BufferedImage for OCR
                int type = BufferedImage.TYPE_BYTE_GRAY;
                BufferedImage image = new BufferedImage(imageRegion.width(), imageRegion.height(), type);
                imageRegion.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());

                // Perform OCR on the cropped region using Tesseract
                ITesseract tesseract = new Tesseract();
                tesseract.setDatapath("/opt/homebrew/Cellar/tesseract/5.3.0_1/share/tessdata");
                try {
                    String ocrResult = tesseract.doOCR(image);
                    System.out.println("OCR result: " + ocrResult);
                } catch (TesseractException e) {
                    throw new RuntimeException(e);
                }
            }else{
                System.out.println("No match found in " + rtspUrl);
            }
        HighGui.imshow(rtspUrl, frame);
        if (HighGui.waitKey(1) == 27) {
            break;
        }
    }
        capture.release();
        HighGui.destroyAllWindows();
    }
}
