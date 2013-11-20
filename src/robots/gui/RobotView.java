package robots.gui;

import org.dobots.utilities.AccelerometerManager;
import org.dobots.utilities.BaseActivity;
import org.dobots.utilities.IAccelerometerListener;
import org.dobots.utilities.ProgressDlg;

import robots.RobotType;
import robots.ctrl.IRemoteRobot;
import robots.ctrl.IRobotDevice;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

public abstract class RobotView extends BaseActivity implements IAccelerometerListener {
	
	protected static final int CONNECT_ID = Menu.FIRST;

	protected static final int GENERAL_GRP = 0;

	protected static String TAG = "RobotDevice";
	
	public static String VIEW_LOADED = "robotview.action.LOADED";
	
	protected BaseActivity m_oActivity;
	protected RobotType m_eRobot;

	protected String m_strRobotID;
	protected Boolean m_bOwnsRobot = false;
	
	private IRobotDevice m_oRobot;

	protected String m_strAddress = "";

	protected ProgressDlg progress;
	
	// Sensitivity towards acceleration
	protected static int SPEED_SENSITIVITY = 10;
	protected static int RADIUS_SENSITIVITY = 400;
	
	protected boolean m_bAccelerometer = false;
	protected boolean m_bSetAccelerometerBase = false;

	protected float m_fXBase, m_fYBase, m_fZBase = 0;

	protected ProgressDialog connectingProgressDialog;

	protected boolean btErrorPending = false;
	
	public RobotView(BaseActivity i_oOwner) {
		m_oActivity = i_oOwner;
	}
	
	public RobotView() {
	}

	protected final Handler m_oUiHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			handleUIMessage(msg);
		}
	};
	
	protected void handleUIMessage(Message msg) {
		switch (msg.what) {
		case MessageTypes.DISPLAY_TOAST:
			showToast((String)msg.obj, Toast.LENGTH_SHORT);
			break;
			
		case MessageTypes.STATE_CONNECTED:
			connectingProgressDialog.dismiss();
			showToast("Connection OK", Toast.LENGTH_SHORT);
			onConnect();
			break;
			
		case MessageTypes.STATE_DISCONNECTED:
			onDisconnect();
			break;

		case MessageTypes.STATE_CONNECTERROR:
			connectingProgressDialog.dismiss();
		case MessageTypes.STATE_RECEIVEERROR:
		case MessageTypes.STATE_SENDERROR:
	    	if (btErrorPending == false) {
	    		onDisconnect();
				
				btErrorPending = true;
				onConnectError();
	    	}
			break;
		}
	}
	
	protected abstract void onConnect();
	protected abstract void onDisconnect();
	protected abstract void onConnectError();

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	
		this.m_oActivity = this;
		
//		m_eRobot = (RobotType) getIntent().getExtras().get("RobotType");
//        m_strRobotID = (String) getIntent().getExtras().get("RobotID");
//        m_bOwnsRobot = (Boolean) getIntent().getExtras().get("OwnsRobot");
        
//        m_oRobot = RobotInventory.getInstance().getRobot(m_strRobotID);
		
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		
        setProperties(m_eRobot);
        
        onLoad();
	}
    
    private void onLoad() {
    	Intent intent = new Intent(RobotView.VIEW_LOADED);
    	intent.putExtra("RobotType", m_eRobot);
    	m_oActivity.sendBroadcast(intent);
    }
    
    @Override
    protected void onStart() {
    	super.onStart();

    }
    
	@Override
	protected void onResume() {
		super.onResume();
		if (AccelerometerManager.isSupported(this)) {
			AccelerometerManager.startListening(this, this);
		}

		if (getSensorGatherer() != null) {
			getSensorGatherer().startThread();
		}
	}
	
    @Override
    public void onPause() {
    	super.onPause();

    	m_bAccelerometer = false;
    }

    @Override
    protected void onStop() {
    	super.onStop();

		if (getSensorGatherer() != null) {
			getSensorGatherer().pauseThread();
		}
    	
    	if (m_bOwnsRobot) {
    		disconnect();
    	}
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();

		if (getSensorGatherer() != null) {
			getSensorGatherer().stopThread();
		}

    	shutDown();
    	
		if (AccelerometerManager.isListening()) {
			AccelerometerManager.stopListening();
		}
    }


    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(GENERAL_GRP, CONNECT_ID, 1, "Connect");
		
		return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case CONNECT_ID:
			resetLayout();
			updateButtons(false);
			disconnect();
			connectToRobot();
			return true;
		}
		
		return super.onMenuItemSelected(featureId, item);
    }
    
	@Override
	public void onAccelerationChanged(float x, float y, float z, boolean tx) {
		if (tx && m_bAccelerometer) {
			
			if (m_bSetAccelerometerBase) {
				if (y > 0 && z > 0) {
					m_fXBase = x;
					m_fYBase = y;
					m_fZBase = z;
				} else {
					m_bAccelerometer = false;
					String strText = "Please hold the phone upwards and try again";
					showToast(strText, strText.length());
				}
				
				m_bSetAccelerometerBase = false;
			}
		}
	}
//
//    protected int getSpeedFromAcceleration(float x, float y, float z, float i_fMaxSpeed) {
//		
//		float speed_off = 0.0F;
//		
//		// calculate speed, we use the angle between the start position
//		// (the position in which the phone was when the acceleration was
//		// turned on) and the current position. 
//		if (x + y > 0) {
//			speed_off = (z - m_fZBase);
//			
//		} else {
//			speed_off = ((9.9F + 9.9F - z) - m_fZBase);
//		}
//
//		// instead of mapping the speed to the range 0..i_nMaxSpeed we add the speed 
//		// sensitivity so that speeds between 0..speed_sensitivity are ignored
//		// before giving the speed as parameter to the drive function we need
//		// to get rid of the speed_sensitivity again.
//		int speed = (int) (speed_off * ((i_fMaxSpeed + SPEED_SENSITIVITY) / 9.9F));
//
//		// cap the speed to [-i_nMaxSpeed,i_nMaxSpeed]
//		speed = Math.max(speed, -(int)i_fMaxSpeed - SPEED_SENSITIVITY);
//		speed = Math.min(speed, (int)i_fMaxSpeed + SPEED_SENSITIVITY);
//
//		return speed;
//    }
//    
	
    protected int getSpeedFromAcceleration(float x, float y, float z, float i_fMaxSpeed, boolean i_bIncludeSensitivity) {
		
		float speed_off = 0.0F;

		Log.i("Accel", "xb=" + m_fXBase + ", yb=" + m_fYBase + ", zb=" + m_fZBase);
		
		Log.i("Accel", "x=" + x + ", y=" + y + ", z=" + z);
		
		// calculate speed, we use the angle between the start position
		// (the position in which the phone was when the acceleration was
		// turned on) and the current position. 
		if (Math.abs(x) + y > 0) {
			speed_off = (z - m_fZBase);
		} else {
			speed_off = ((9.9F + 9.9F - z) - m_fZBase);
		}

		int speed = 0;
		
		if (i_bIncludeSensitivity) {
			// instead of mapping the speed to the range 0..i_nMaxSpeed we add the speed 
			// sensitivity so that speeds between 0..speed_sensitivity are ignored
			// before giving the speed as parameter to the drive function we need
			// to get rid of the speed_sensitivity again.
			speed = (int) (speed_off * ((i_fMaxSpeed + SPEED_SENSITIVITY) / 9.9F));
	
			// cap the speed to [-i_nMaxSpeed,i_nMaxSpeed]
			speed = Math.max(speed, -(int)i_fMaxSpeed - SPEED_SENSITIVITY);
			speed = Math.min(speed, (int)i_fMaxSpeed + SPEED_SENSITIVITY);
		} else {
			// instead of mapping the speed to the range 0..i_nMaxSpeed we add the speed 
			// sensitivity so that speeds between 0..speed_sensitivity are ignored
			// before giving the speed as parameter to the drive function we need
			// to get rid of the speed_sensitivity again.
			speed = (int) (speed_off * ((i_fMaxSpeed) / 9.9F));
	
			// cap the speed to [-i_nMaxSpeed,i_nMaxSpeed]
			speed = Math.max(speed, -(int)i_fMaxSpeed);
			speed = Math.min(speed, (int)i_fMaxSpeed);
		}
		
		return speed;
    }
    
    protected int getRadiusFromAcceleration(float x, float y, float z, float i_fMaxRadius) {

    	float radius_off;
    	
		// convert to [-i_nMaxRadius,i_nMaxRadius]
    	if (x < 0 || Math.abs(z) + y > 0) {
    		radius_off = (x - m_fXBase);
			
		} else {
			radius_off = ((9.9F + 9.9F - x) - m_fXBase);
		}

		return (int) (radius_off * (i_fMaxRadius / 9.9F) * 1.5); // factor 2 added to make it react faster

    }
    
    public Handler getUIHandler() {
    	return m_oUiHandler;
    }

	protected IRobotDevice getRobot() {
		return m_oRobot;
	}
	
	protected void setRobot(IRobotDevice robot) {
		m_oRobot = robot;
	}

    @Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    		//nothing to be done right now here
    	}
	}

	protected abstract void setProperties(RobotType i_eRobot);
	
	protected void shutDown() {
		
//		if (m_bOwnsRobot) {
//			getRobot().destroy();
//		}
		
//		if (!m_bKeepAlive) {
//			getRobot().destroy();
//		}
	}
	
    protected void sendBundle(Handler i_oHandler, Bundle i_oBundle) {
        Message myMessage = new Message();
        myMessage.setData(i_oBundle);
        i_oHandler.sendMessage(myMessage);
    }

	public void showConnectingDialog() {
    	connectingProgressDialog = ProgressDialog.show(m_oActivity, "", "Connecting to the robot.\nPlease wait...");
    }
    
    protected static ProgressDialog showConnectingDialog(Context i_oContext) {
    	return ProgressDialog.show(i_oContext, "", "Connecting to the robot.\nPlease wait...");
    }

	protected abstract void connectToRobot();
	
	protected abstract void disconnect();
	
	protected abstract void resetLayout();
	protected abstract void updateButtons(boolean i_bEnabled);
	
	protected abstract SensorGatherer getSensorGatherer();
	
	public static String getMacFilter() {
		// has to be implemented by child class
		return "";
	}

}
