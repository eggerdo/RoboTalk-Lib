package robots.piratedotty.gui;

import java.io.IOException;

import org.dobots.R;
import org.dobots.utilities.BaseActivity;
import org.dobots.utilities.Utils;
import org.dobots.utilities.camera.CameraPreview;

import robots.RobotType;
import robots.ctrl.control.ICameraControlListener;
import robots.ctrl.zmq.ZmqRemoteControlHelper;
import robots.ctrl.zmq.ZmqRemoteControlSender;
import robots.gui.BluetoothRobot;
import robots.gui.RobotInventory;
import robots.gui.SensorGatherer;
import robots.gui.comm.IConnectListener;
import robots.gui.comm.IRobotConnection;
import robots.gui.comm.bluetooth.BluetoothConnection;
import robots.piratedotty.ctrl.PirateDotty;
import robots.piratedotty.ctrl.PirateDottyTypes;
import android.bluetooth.BluetoothDevice;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class PirateDottyRobot extends BluetoothRobot implements ICameraControlListener {

	private static String TAG = "PirateDotty";
	
	private static final int CONNECT_ID = Menu.FIRST;
	private static final int ACCEL_ID = CONNECT_ID + 1;
	private static final int REMOTE_CONTROL_ID = ACCEL_ID + 1;
	private static final int CAMERA_ID = REMOTE_CONTROL_ID + 1;
	private static final int CAMERA_DISP_ID = CAMERA_ID + 1;

	private static final int CONTROL_GRP = GENERAL_GRP + 1;
//	private static final int REMOTE_CTRL_GRP = GENERAL_GRP + 1;
	private static final int CAMERA_CTRL_GRP = CONTROL_GRP + 1;
	
	private PirateDotty m_oPirateDotty;

	private PirateDottySensorGatherer m_oSensorGatherer;

	private CameraPreview m_oCamera;
	
	private ImageButton m_btnCameraToggle;

	private ZmqRemoteControlSender m_oZmqRemoteSender;
	private ZmqRemoteControlHelper m_oRemoteCtrl;
	private ZmqRemoteControlHelper m_oCameraCtrl;
	
	private boolean m_bCameraOn = true;
	
	public PirateDottyRobot(BaseActivity i_oOwner) {
		super(i_oOwner);
	}
	
	public PirateDottyRobot() {
		super();
	}
	
	protected SensorGatherer getSensorGatherer() {
		return m_oSensorGatherer;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        
    	m_oPirateDotty = (PirateDotty) getRobot();
    	m_oPirateDotty.setHandler(m_oUiHandler);

    	m_oZmqRemoteSender = new ZmqRemoteControlSender(m_oPirateDotty.getID());

    	// remote control helper, handles ui buttons and sends commands over zmq
		m_oRemoteCtrl = new ZmqRemoteControlHelper(m_oActivity);
		m_oRemoteCtrl.setDriveControlListener(m_oZmqRemoteSender);
		
		// receives and handles incoming camera control commands
		m_oCameraCtrl = new ZmqRemoteControlHelper();
		m_oCameraCtrl.setCameraControlListener(this);
		m_oCameraCtrl.startReceiver("PirateDottyUI");

        m_oSensorGatherer = new PirateDottySensorGatherer(this, m_strRobotID);
		m_oCamera.setFrameListener(m_oSensorGatherer);

        updateButtons(false);

        if (m_oPirateDotty.isConnected()) {
			updateButtons(true);
		} else {
			connectToRobot();
		}
    }
	
	@Override
	public void onDestroy() {
		m_oCamera.stopCamera();
		m_oRemoteCtrl.destroy();
		m_oZmqRemoteSender.close();
		
		if (m_bOwnsRobot) {
			RobotInventory.getInstance().removeRobot(m_strRobotID);
		}
		
		super.onDestroy();
	}

	@Override
	public void handleUIMessage(Message msg) {
		super.handleUIMessage(msg);
		
		switch (msg.what) {
		case PirateDottyTypes.SENSOR_DATA:
			m_oSensorGatherer.sendMessage(PirateDottyTypes.SENSOR_DATA, msg.obj);
			break;
		}
	}
	
    @Override
	protected void setProperties(RobotType i_eRobot) {
        m_oActivity.setContentView(R.layout.robot_piratedotty_main);

		m_oCamera = (CameraPreview) findViewById(R.id.svCamera);
		m_oCamera.setScale(false);
		m_oCamera.setPreviewSize(640, 480);
		m_oCamera.setHidden();
	
		m_btnCameraToggle = (ImageButton) findViewById(R.id.btnCameraToggle);
		if (Camera.getNumberOfCameras() <= 1) {
			m_btnCameraToggle.setVisibility(View.GONE);
		} else {
			m_btnCameraToggle.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					toggleCamera();
					m_oPirateDotty.toggleInvertDrive();
				}
			});
		}
    }

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

//		menu.add(REMOTE_CTRL_GRP, ACCEL_ID, ACCEL_ID, "Accelerometer");
		menu.add(CONTROL_GRP, REMOTE_CONTROL_ID, REMOTE_CONTROL_ID, "Remote Control");
		menu.add(CONTROL_GRP, CAMERA_ID, CAMERA_ID, "Camera");
		menu.add(CAMERA_CTRL_GRP, CAMERA_DISP_ID, CAMERA_DISP_ID, "Display Camera");
		
		return true;
	}

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	
    	menu.setGroupVisible(CONTROL_GRP, m_oPirateDotty.isConnected());
    	menu.setGroupVisible(CAMERA_CTRL_GRP, m_oPirateDotty.isConnected() && m_bCameraOn );

//    	Utils.updateOnOffMenuItem(menu.findItem(ACCEL_ID), m_bAccelerometer);
    	Utils.updateOnOffMenuItem(menu.findItem(REMOTE_CONTROL_ID), m_oRemoteCtrl.isControlEnabled());
    	Utils.updateOnOffMenuItem(menu.findItem(CAMERA_ID), m_bCameraOn);
    	Utils.updateOnOffMenuItem(menu.findItem(CAMERA_DISP_ID), !m_oCamera.isHidden());

    	return true;
    }
    
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case ACCEL_ID:
			m_bAccelerometer = !m_bAccelerometer;

			if (m_bAccelerometer) {
				m_bSetAccelerometerBase = true;
			} else {
				m_oPirateDotty.moveStop();
			}
			break;
		case REMOTE_CONTROL_ID:
			m_oRemoteCtrl.setRemoteControl(!m_oRemoteCtrl.isControlEnabled());
			break;
		case CAMERA_ID:
			if (m_bCameraOn) {
				switchCameraOff();
			} else {
				switchCameraOn();
			}
			m_bCameraOn = !m_bCameraOn;
			break;
		case CAMERA_DISP_ID:
			if (m_oCamera.isHidden()) {
				showCameraDisplay();
			} else {
				hideCameraDisplay();
			}
			break;
		}

		return super.onMenuItemSelected(featureId, item);
	}

	protected void resetLayout() {
        m_oRemoteCtrl.resetLayout();
        
        updateButtons(false);

		m_oSensorGatherer.initialize();
	}
	
	public void updateButtons(boolean enabled) {
		m_oRemoteCtrl.setControlEnabled(enabled);
	}

	@Override
	protected void onConnect() {
		updateButtons(true);
	}
	
	@Override
	protected void onDisconnect() {
		updateButtons(false);
		m_oRemoteCtrl.resetLayout();
	}

	@Override
	protected void disconnect() {
		m_oPirateDotty.disconnect();
	}
	
	@Override
	public void connect(BluetoothDevice i_oDevice) {
		m_strAddress = i_oDevice.getAddress();
		showConnectingDialog();
		
		if (m_oPirateDotty.getConnection() != null) {
			try {
				m_oPirateDotty.getConnection().close();
			}
			catch (IOException e) { }
		}
		IRobotConnection connection = new BluetoothConnection(i_oDevice, PirateDottyTypes.PIRATEDOTTY_UUID);
		connection.setReceiveHandler(m_oUiHandler);
		m_oPirateDotty.setConnection(connection);
		m_oPirateDotty.connect();
	}

	public static void connectToPirateDotty(final BaseActivity m_oOwner, PirateDotty i_oPirateDotty, BluetoothDevice i_oDevice, final IConnectListener i_oConnectListener) {
		PirateDottyRobot m_oRobot = new PirateDottyRobot(m_oOwner) {
			public void onConnect() {
				i_oConnectListener.onConnect(true);
			};
			public void onDisconnect() {
				i_oConnectListener.onConnect(false);
			};
		};
		
		m_oRobot.showConnectingDialog();
		
		if (i_oPirateDotty.isConnected()) {
			i_oPirateDotty.disconnect();
		}

		i_oPirateDotty.setHandler(m_oRobot.getUIHandler());
		IRobotConnection connection = new BluetoothConnection(i_oDevice, PirateDottyTypes.PIRATEDOTTY_UUID);
		connection.setReceiveHandler(m_oRobot.getUIHandler());
		i_oPirateDotty.setConnection(connection);
		i_oPirateDotty.connect();
	}

	@Override
	public void toggleCamera() {
		// toggle camera only works if it is executed by the UI thread
		// so we check if the calling thread is the main thread, otherwise
		// we call the function again inside the main thread.
		if (Looper.myLooper() != Looper.getMainLooper()) {
			Utils.runAsyncUiTask(new Runnable() {
				@Override
				public void run() {
					toggleCamera();
				}
			});
		} else {
			m_oCamera.toggleCamera();
		}
	}

	@Override
	public void switchCameraOn() {
		m_oCamera.startCamera();
	}

	@Override
	public void switchCameraOff() {
		m_oCamera.stopCamera();
	}
	
	public void showCameraDisplay() {
		m_oCamera.showCameraDisplay();
	}
	
	public void hideCameraDisplay() {
		m_oCamera.hideCameraDisplay();
	}

	@Override
	public void cameraUp() {
		// NOT APPLICABLE FOR THIS ROBOT
	}

	@Override
	public void cameraDown() {
		// NOT APPLICABLE FOR THIS ROBOT
	}

	@Override
	public void cameraStop() {
		// NOT APPLICABLE FOR THIS ROBOT
	}

}
