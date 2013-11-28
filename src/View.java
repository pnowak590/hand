import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;


public class View {

	private JFrame frame;
	private JPanel imagesPanel;
	private JPanel imageTop;
	private JPanel imageBottom;
	
	private Mat videoFrame;
	private Mat computedFrame;
	private MatOfByte videoFrameBytes;
	private VideoCapture vc;
	
	private Point handPosition;
	
	public View() {
		initializeGUI();
	}

	private void initializeGUI() {
		frame = new JFrame();
		frame.setBounds(100, 100, 660, 260);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new GridLayout(0, 2, 0, 0));
		
		imageTop = new JPanel();
		imageTop.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		imageTop.setBounds(0, 0, 320, 240);
		frame.add(imageTop);
		imageTop.setLayout(null);
		
		imageBottom = new JPanel();
		imageBottom.setBorder(new LineBorder(new Color(0, 0, 0), 1, true));
		imageBottom.setBounds(0, 240, 320, 240);
		frame.add(imageBottom);
		
		frame.setVisible(true);
		
		showCamera();
	}
	
	private void showCamera() {

		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					videoFrame = new Mat();
					computedFrame = new Mat();
					videoFrameBytes = new MatOfByte();
					vc = new VideoCapture(0);
					
					vc.open(0);
					vc.set(Highgui.CV_CAP_PROP_FRAME_WIDTH , 320);
					vc.set(Highgui.CV_CAP_PROP_FRAME_HEIGHT , 240);
					
					if (!vc.isOpened()) {
						System.out.println("Error Initializing camera");
					} else {
						System.out.println("Camera OK" );
					}
					
					while(vc.grab()) {
						captureImage();
						Thread.sleep(40);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}
	
	private void captureImage() {
		try {
	         vc.retrieve(videoFrame);
	         locate();
	         showFrame();
	         } catch(Exception e) {
	        	 e.printStackTrace();
        }
	}
	
	public void showFrame() throws IOException {
		Highgui.imencode(".png", videoFrame, videoFrameBytes);
        byte[] byteArray = videoFrameBytes.toArray();
        BufferedImage bufImage = null;
	    InputStream in = new ByteArrayInputStream(byteArray);
	    bufImage = ImageIO.read(in);
        imageTop.getGraphics().drawImage(bufImage, 0, 0, null);
        
        Highgui.imencode(".png", computedFrame, videoFrameBytes);
        byteArray = videoFrameBytes.toArray();
        bufImage = null;
	    in = new ByteArrayInputStream(byteArray);
	    bufImage = ImageIO.read(in);
        imageBottom.getGraphics().drawImage(bufImage, 0, 0, null);
	}
	
	public void locate() {
		
//		Imgproc.cvtColor(videoFrame, videoFrame, Imgproc.COLOR_BGR2HSV);
//		Core.inRange(videoFrame, new Scalar(120, 55, 65, 0), new Scalar(180, 256, 256 ,0), computedFrame);
//		Core.inRange(videoFrame, new Scalar(0, 25, 30), new Scalar(20, 150, 180), computedFrame);
		
		Imgproc.cvtColor(videoFrame, videoFrame, Imgproc.COLOR_RGB2HSV);
//		Imgproc.cvtColor(videoFrame, videoFrame, Imgproc.COLOR_RGB2GRAY);
//		Imgproc.equalizeHist(videoFrame, videoFrame);
		Core.inRange(videoFrame, new Scalar(0, 0, 0), new Scalar(40, 60, 256), computedFrame);

		// czarnobia³y
//		Core.inRange(videoFrame, new Scalar(150), new Scalar(255), computedFrame);
		Imgproc.dilate(computedFrame, computedFrame, Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(10, 10)));
		Imgproc.erode(computedFrame, computedFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(10, 10)));

		// Wyliczam œrodek bia³ego pola
		
		int sumx = 0;
		int sumy = 0;
		int sum = 0;
		
		for (int col = 0; col < computedFrame.width(); col++) {
			for (int row = 0; row < computedFrame.height(); row++) {
				if (computedFrame.get(row, col)[0] == 255) {
					sumx += col;
					sumy += row;
					sum += 1;
				}
			}
		}
		
		if (sum > 0)
			handPosition = new Point(sumx / sum, sumy / sum);
		else
			handPosition = new Point(0, 0);
		
		Core.circle(videoFrame, handPosition, 10, new Scalar(255,0,0), 5);
	}
}
