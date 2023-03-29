public class OpencvTest {

        public static void main(String[] args) {
            String[] rtspUrls = {"rtsp://localhost:8554/live4466/test"};
            for (String rtspUrl : rtspUrls) {
                RTSPThread thread = new RTSPThread(rtspUrl);
                thread.start();
            }
        }
    }
//"rtsp://localhost:8554/live4466/test2"
