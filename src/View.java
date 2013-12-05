import java.awt.AWTException;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import java.awt.Robot;

public class View {

	private JFrame frame;
	private JPanel imageTop;
	private JPanel imageBottom;
	
	private Mat videoFrame;
	private Mat videoFramePrev;
	private Mat computedFrame;
	private MatOfByte videoFrameBytes;
	private VideoCapture vc;
	
	private Point handPosition = new Point();
	private Point prevHandPosition = new Point();
	private int move = 0;
	
	long lastEventTime;
	long interval = 2000;
	
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
		
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {

	        public void run() {
	        	System.out.println("On exit");
	            if (vc!=null)
	            	vc.release();
	        }
	    }));
		
		
		showCamera();
	}
	
	private void showCamera() {

		Runnable r = new Runnable() {
			@Override
			public void run() {
				try {
					videoFrame = new Mat();
					videoFramePrev = new Mat();
					computedFrame = new Mat();
					videoFrameBytes = new MatOfByte();
					vc = new VideoCapture(0);
					
					Thread.sleep(1000);
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
					vc.release();
					e.printStackTrace();
				}
			}
		};
		
		Thread t = new Thread(r);
		t.start();
	}
	
	private void captureImage() {
		try {
			videoFramePrev = videoFrame.clone();
			vc.retrieve(videoFrame);
			locate();
			
			moveWithInterval();
			//move();
			
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
		
		// rozmazuj� aby nie by�o szum�w
		Imgproc.blur(videoFrame, videoFrame, new Size(9,9));
		
		// r�nica klatek
		Core.subtract(videoFramePrev, videoFrame, computedFrame);

		Imgproc.cvtColor(computedFrame, computedFrame, Imgproc.COLOR_RGB2GRAY);
		
		// rozszerzenie i erozja - zmniejszenie szum�w
		Imgproc.dilate(computedFrame, computedFrame, Imgproc.getStructuringElement(Imgproc.MORPH_DILATE, new Size(10, 10)));
		Imgproc.erode(computedFrame, computedFrame, Imgproc.getStructuringElement(Imgproc.MORPH_ERODE, new Size(10, 10)));

		Core.inRange(computedFrame, new Scalar(50), new Scalar(255), computedFrame);
		
		// Wyliczam �rodek bia�ego pola
		int sumx = 0;
		int sumy = 0;
		int sum = 0;
		
		for (int col = 0; col < computedFrame.width(); col++) {
			for (int row = 0; row < computedFrame.height(); row++) {
				if (computedFrame.get(row, col)[0] > 0) {
					sumx += col;
					sumy += row;
					sum += 1;
				}
			}
		}
		
		prevHandPosition = handPosition.clone();
		
		if (sum > (computedFrame.width() * computedFrame.height()) * 0.02) {
			handPosition = new Point(sumx / sum, sumy / sum);
		}
		else {
			handPosition = new Point(0, 0);
			move = 0;
		}
		
		// rysuj� k�ko
		Imgproc.cvtColor(computedFrame, computedFrame, Imgproc.COLOR_GRAY2RGB);
		Core.circle(computedFrame, handPosition, 10, new Scalar(255,0,0), 5);
	}
	
	private void move() {
		String direction = new String();
		
		if (handPosition.x != 0 && prevHandPosition.x != 0) {
			move += prevHandPosition.x - handPosition.x;
		}
		
		if (move < -100) {
			direction = "right";
			nextSlide();
		} else if (move > 100) {
			direction = "left";
			prevSlide();
		}
		
		Core.putText(computedFrame, "move: " + move + "  " + direction, 
				new Point(20,20), 0, 0.3, new Scalar(0,255,0));
	}
	
	private void moveWithInterval() {
		long curr = System.currentTimeMillis();
		if (curr > (lastEventTime + interval)) {
			move();
		} else {
			System.out.println("Damn! U re 2 fast!");
		}
	}
	
	private void nextSlide() {
		System.out.println("next slide >");
		
		try {
			Robot r = new Robot();
			
			r.keyPress(KeyEvent.VK_RIGHT);
			lastEventTime = System.currentTimeMillis();
			
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
	}
	
	private void prevSlide() {
		System.out.println("< prev slide");
		
		try {
			Robot r = new Robot();
			
			r.keyPress(KeyEvent.VK_LEFT);
			lastEventTime = System.currentTimeMillis();
			
		} catch (AWTException e) {
			e.printStackTrace();
		}
		
	}
}
