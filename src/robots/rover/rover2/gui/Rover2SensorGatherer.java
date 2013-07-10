package robots.rover.rover2.gui;

import org.dobots.communication.video.VideoDisplayThread;
import org.dobots.communication.video.VideoDisplayThread.FPSListener;
import org.dobots.communication.video.VideoDisplayThread.VideoListener;
import org.dobots.communication.zmq.ZmqHandler;
import org.dobots.utilities.BaseActivity;
import org.dobots.utilities.Utils;
import org.zeromq.ZMQ;

import robots.rover.gui.RoverBaseSensorGatherer;
import robots.rover.rover2.ctrl.Rover2;
import android.graphics.Bitmap;

public class Rover2SensorGatherer extends RoverBaseSensorGatherer  implements VideoListener, FPSListener {

	private VideoDisplayThread m_oVideoDisplayer;

	public Rover2SensorGatherer(BaseActivity i_oActivity, Rover2 i_oRover) {
		super(i_oActivity, i_oRover, "Rover2SensorGatherer");
	}

	@Override
	protected void startVideo() {
		super.startVideo();
		setVideoListening(true);
	}

	@Override
	protected void stopVideo() {
		super.stopVideo();
		setVideoListening(false);
	}
	
	private void setVideoListening(boolean i_bListening) {
		if (i_bListening) {
			setupVideoDisplay();
		} else {
			if (m_oVideoDisplayer != null) {
				m_oVideoDisplayer.close();
			}
		}
	}

    private void setupVideoDisplay() {
    	
		ZMQ.Socket oVideoRecvSocket = ZmqHandler.getInstance().obtainVideoRecvSocket();
		oVideoRecvSocket.subscribe(m_oRover.getID().getBytes());

		// start a video display thread which receives video frames from the socket and displays them
		m_oVideoDisplayer = new VideoDisplayThread(ZmqHandler.getInstance().getContext().getContext(), oVideoRecvSocket);
		m_oVideoDisplayer.setVideoListner(this);
		m_oVideoDisplayer.setFPSListener(this);
		m_oVideoDisplayer.start();
	}

	@Override
	public void onFPS(final int i_nFPS) {
		Utils.runAsyncUiTask(new Runnable() {

			@Override
			public void run() {
				if (!m_bVideoStopped) {
					m_lblFPS.setText("FPS: " + String.valueOf(i_nFPS));
				}
			}
		});
	}

	@Override
	public void onFrame(final Bitmap i_oBmp, int i_nRotation) {

		Utils.runAsyncUiTask(new Runnable() {

			@Override
			public void run() {
				if (!m_bVideoStopped) {
					if (!m_bVideoConnected) {
						m_oSensorDataUiUpdater.removeCallbacks(m_oTimeoutRunnable);
						m_bVideoConnected = true;
						showVideoLoading(false);
					}

					m_ivVideo.setImageBitmap(i_oBmp);
				}
			}
			
		});
	}
	
	@Override
	public void shutDown() {
		if (m_oVideoDisplayer != null) {
			m_oVideoDisplayer.close();
		}
	}
    
}