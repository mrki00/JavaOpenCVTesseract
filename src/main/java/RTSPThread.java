import org.opencv.core.*;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import net.sourceforge.tess4j.*;
import java.awt.image.DataBufferByte;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class RTSPThread extends Thread {
    private String rtspUrl;

    public static void searchPositions(String position, Mat imgRegion){
        // Extract the position, size, and color for teamName1
        String Position = sectionData.get(position + "Position");
        String Size = sectionData.get(position + "Size");
        String Color = sectionData.get(position + "Color");

        // Parse the position and size to get the x, y, width, and height
        int x = Integer.parseInt(Position.substring(1, Position.indexOf('y')));
        int y = Integer.parseInt(Position.substring(Position.indexOf('y') + 1));
        int width1 = Integer.parseInt(Size.substring(0, Size.indexOf('x')));
        int height1 = Integer.parseInt(Size.substring(Size.indexOf('x') + 1));

        // Create a submat for the position section
        Mat section = imgRegion.submat(y, y + height1, x, x + width1);

        // Convert the teamName1 section to a BufferedImage for OCR
        int type1 = BufferedImage.TYPE_BYTE_GRAY;
        BufferedImage teamName1Image = new BufferedImage(section.width(), section.height(), type1);
        section.get(0, 0, ((DataBufferByte) teamName1Image.getRaster().getDataBuffer()).getData());

        // Perform OCR on the position section using Tesseract
        ITesseract tesseract = new Tesseract();
        tesseract.setDatapath("/opt/homebrew/Cellar/tesseract/5.3.1_1/share/tessdata");
        tesseract.setLanguage("hrv");

        try {
            String ocrResult = tesseract.doOCR(teamName1Image);
            System.out.println("OCR result for" + position +": " + ocrResult);
        } catch (TesseractException e) {
            throw new RuntimeException(e);
        }
    }
    public RTSPThread(String rtspUrl) {
        this.rtspUrl = rtspUrl;
    }
    static Map<String, String> sectionData = new HashMap<>();
    public List<String> positions = List.of("teamName1", "teamScore1","teamName2", "teamScore2", "time");
    @Override
    public void run() {
        sectionData.put("threshold", "0.65");
        sectionData.put("expectedGfxX", "460");
        sectionData.put("expectedGfxY", "46");
        sectionData.put("teamName1Position", "x12y2");
        sectionData.put("teamName1Size", "68x34");
        sectionData.put("teamName1Color", "black");
        sectionData.put("teamScore1Position", "x98y6");
        sectionData.put("teamScore1Size", "34x30");
        sectionData.put("teamScore1Color", "white");
        sectionData.put("teamName2Position", "x280y2");
        sectionData.put("teamName2Size", "68x34");
        sectionData.put("teamName2Color", "black");
        sectionData.put("teamScore2Position", "x228y6");
        sectionData.put("teamScore2Size", "34x30");
        sectionData.put("teamScore2Color", "white");
        sectionData.put("timePosition", "x156y26");
        sectionData.put("timeSize", "50x18");
        sectionData.put("timeColor", "white");



        System.out.println("in run");
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        System.setProperty("jna.library.path", "/opt/homebrew/Cellar/tesseract/5.3.1_1/lib/");

        // VideoCapture capture = new VideoCapture();
        // capture.open(rtspUrl);
        // if (!capture.isOpened()) {
        //     System.err.println("Failed to open RTSP stream: " + rtspUrl);
        //     return;
        // }
        // capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
        // capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);

        Mat frame;
        // while (true) {
        //     frame = new Mat();

        //     capture.read(frame);
        //     if (frame.empty()) {
        //         System.err.println("Failed to read frame from RTSP stream: " + rtspUrl);
        //         try {
        //             Thread.sleep(5000);
        //             capture.release();
        //             capture.open(rtspUrl);
        //             continue;
        //         } catch (InterruptedException e) {
        //             capture.release();
        //         }
        //     }
        frame = Imgcodecs.imread("/Users/mrki/Downloads/testFrame.png");

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        Mat exampleImage = Imgcodecs.imread("/Users/mrki/Downloads/score720.png");
        Mat grayExampleImage = new Mat();
        Imgproc.cvtColor(exampleImage, grayExampleImage, Imgproc.COLOR_BGR2GRAY);
        int width = grayExampleImage.width();
        int height = grayExampleImage.height();
        Mat searchArea = frame.submat(Integer.parseInt(sectionData.get("expectedGfxY")), Integer.parseInt(sectionData.get("expectedGfxY")) + height, Integer.parseInt(sectionData.get("expectedGfxX")), Integer.parseInt(sectionData.get("expectedGfxX")) + width);
        Mat graySearchArea = new Mat();
        Imgproc.cvtColor(searchArea, graySearchArea, Imgproc.COLOR_BGR2GRAY);
        Mat matchResult = new Mat();
        Imgproc.matchTemplate(graySearchArea, grayExampleImage, matchResult, Imgproc.TM_CCOEFF_NORMED);
        Core.MinMaxLocResult mmr = Core.minMaxLoc(matchResult);
        Point matchLoc = mmr.maxLoc;
        if (mmr.maxVal > Double.parseDouble(sectionData.get("threshold"))) {
            System.out.println(mmr.maxVal + " found in " + rtspUrl);
            Point pt1 = new Point(matchLoc.x + Integer.parseInt(sectionData.get("expectedGfxX")), matchLoc.y + Integer.parseInt(sectionData.get("expectedGfxY")));
            Point pt2 = new Point(matchLoc.x + Integer.parseInt(sectionData.get("expectedGfxX")) + width, matchLoc.y + Integer.parseInt(sectionData.get("expectedGfxY")) + height);
            Imgproc.rectangle(frame, pt1, pt2, new Scalar(0, 0, 255), 2);

            // Crop the search area to just the region where the image is found
            Mat imageRegion = graySearchArea.submat((int) matchLoc.y, (int) (matchLoc.y + height), (int) matchLoc.x, (int) (matchLoc.x + width));

            // Convert the cropped region to a BufferedImage for OCR
            int type = BufferedImage.TYPE_BYTE_GRAY;
            BufferedImage image = new BufferedImage(imageRegion.width(), imageRegion.height(), type);
            imageRegion.get(0, 0, ((DataBufferByte) image.getRaster().getDataBuffer()).getData());
            

            for(int i = 0; i < positions.size(); i++){
                searchPositions(positions.get(i), imageRegion);
            }


        }else{
            System.out.println("No match found in " + rtspUrl);
        }
        HighGui.imshow(rtspUrl, frame);

        if (HighGui.waitKey(1) == 27) {
            // break;
        }
        // }
        // capture.release();
        HighGui.destroyAllWindows();
    }
}
