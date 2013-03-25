package org.dobots.robotalk.control;

import org.dobots.robotalk.R;
import org.dobots.utilities.LockableScrollView;
import org.dobots.utilities.Utils;
import org.dobots.utilities.joystick.IJoystickListener;
import org.dobots.utilities.joystick.Joystick;

import robots.IRobotDevice;
import robots.RobotRemoteListener;
import android.app.Activity;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

public class RemoteControlHelper implements IJoystickListener {

	static final String TAG = "RemoteControlHelper";
	
	public enum Move {
		NONE, STRAIGHT_FORWARD, FORWARD, STRAIGHT_BACKWARD, BACKWARD, ROTATE_LEFT, ROTATE_RIGHT
	}
	
	protected IRemoteControlListener m_oRemoteControlListener = null;
	
	private Move lastMove = Move.NONE;

	private long lastTime = SystemClock.uptimeMillis();
	private double updateFrequency = 5.0; // Hz
	private int threshold = 20;
	
	private Activity m_oActivity;
	
	private boolean m_bControl;
	private boolean m_bAdvancedControl = true;

	private ToggleButton m_btnControl;
	private Button m_btnFwd;
	private Button m_btnBwd;
	private Button m_btnLeft;
	private Button m_btnRight;
	
	private LockableScrollView m_oScrollView;
	private LinearLayout m_oAdvancedControl;
	
	private Joystick m_oJoystick;
	
	protected IRobotDevice m_oRobot;
	
	protected RemoteControlHelper(Activity i_oActivity) {
		this.m_oActivity = i_oActivity;
	}

	// At least one of the parameters i_oRobot or i_oListener has to be assigned! the other can be null.
	// It is also possible to assign both
	public RemoteControlHelper(Activity i_oActivity, IRobotDevice i_oRobot, IRemoteControlListener i_oListener) {
		this(i_oActivity);
		m_oRobot = i_oRobot;
		
		// one of the two parameters, RobotDevice or RemoteControlListener has to be assigned!
		assert(!(i_oRobot == null && i_oListener == null));
		
		if (i_oRobot != null) {
			m_oRemoteControlListener = new RobotRemoteListener(i_oRobot);
		}
		
		// by default, this class is handling the move commands triggered either by the remote control buttons
		// or by the joystick. However it is possible to overwrite the listener so that move commands
		// can be individually handled
		if (i_oListener != null) {
			m_oRemoteControlListener = i_oListener;
		}
	}
	
	public void setRemoteControlListener(IRemoteControlListener i_oListener) {
		m_oRemoteControlListener = i_oListener;
	}
	
	public void removeRemoteControlListener(IRemoteControlListener i_oListener) {
		if (m_oRemoteControlListener == i_oListener) {
			m_oRemoteControlListener = null;
		}
	}
	
	public boolean hasControlButton() {
		return m_btnControl != null;
	}
	
	public void setProperties() {
		
		m_oScrollView = (LockableScrollView) m_oActivity.findViewById(R.id.scrollview);
		
		m_oAdvancedControl = (LinearLayout) m_oActivity.findViewById(R.id.layAdvancedControl);
		
		m_oJoystick = (Joystick) m_oActivity.findViewById(R.id.oJoystick);
		m_oJoystick.setUpdateListener(this);

		m_btnControl = (ToggleButton) m_oActivity.findViewById(R.id.btnRemoteControl);
		if (hasControlButton()) {
			m_btnControl.setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					m_bControl = !m_bControl;
					enableControl(m_bControl);
					showControlButtons(m_bControl);
				}
			});
		}
	
		m_btnFwd = (Button) m_oActivity.findViewById(R.id.btnFwd);
		m_btnLeft = (Button) m_oActivity.findViewById(R.id.btnLeft);
		m_btnBwd = (Button) m_oActivity.findViewById(R.id.btnBwd);
		m_btnRight = (Button) m_oActivity.findViewById(R.id.btnRight);
		
		m_btnFwd.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (m_oRemoteControlListener != null) {
					int action = e.getAction();
					switch (action & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						onMove(Move.NONE);
						break;
					case MotionEvent.ACTION_POINTER_UP:
						break;
					case MotionEvent.ACTION_DOWN:
						onMove(Move.STRAIGHT_FORWARD);
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						break;					
					case MotionEvent.ACTION_MOVE:
						break;
					}
				}
				return true;
			}
		});
		
		m_btnBwd.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (m_oRemoteControlListener != null) {
					int action = e.getAction();
					switch (action & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						onMove(Move.NONE);
						break;
					case MotionEvent.ACTION_POINTER_UP:
						break;
					case MotionEvent.ACTION_DOWN:
						onMove(Move.STRAIGHT_BACKWARD);
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						break;					
					case MotionEvent.ACTION_MOVE:
						break;
					}
				}
				return true;
			}
		});

		m_btnLeft.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (m_oRemoteControlListener != null) {
					int action = e.getAction();
					switch (action & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						onMove(Move.NONE);
						break;
					case MotionEvent.ACTION_POINTER_UP:
						break;
					case MotionEvent.ACTION_DOWN:
						onMove(Move.ROTATE_LEFT);
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						break;					
					case MotionEvent.ACTION_MOVE:
						break;
					}
				}
				return true;
			}
		});

		m_btnRight.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent e) {
				if (m_oRemoteControlListener != null) {
					int action = e.getAction();
					switch (action & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						onMove(Move.NONE);
						break;
					case MotionEvent.ACTION_POINTER_UP:
						break;
					case MotionEvent.ACTION_DOWN:
						onMove(Move.ROTATE_RIGHT);
						break;
					case MotionEvent.ACTION_POINTER_DOWN:
						break;					
					case MotionEvent.ACTION_MOVE:
						break;
					}
				}
				return true;
			}
		});
		
		resetLayout();
	}
	
	public void showControlButtons(boolean visible) {
		if (!visible) {
			Utils.showLayout((LinearLayout)m_oActivity.findViewById(R.id.layRemoteControl), visible);
			if (m_bAdvancedControl) {
				Utils.showLayout(m_oAdvancedControl, visible);
			}
		} else {
			Utils.showLayout((LinearLayout)m_oActivity.findViewById(R.id.layRemoteControl), visible);
			if (m_bAdvancedControl) {
				Utils.showLayout(m_oAdvancedControl, visible);
			} 
		}
	}
	
	public void resetLayout() {
		if (hasControlButton()) {
			m_btnControl.setChecked(false);
			showControlButtons(false);
		} else {
			showControlButtons(true);
		}
		m_bControl = false;
		updateButtons(false);
	}

	public void updateButtons(boolean enabled) {
		if (hasControlButton()) {
			m_btnControl.setEnabled(enabled);
		}
	}
	
	@Override
	public void onJoystickTouch(boolean start) {
		if (m_oScrollView != null) {
			if (start) {
				m_oScrollView.setScrollingEnabled(false);
			} else {
				m_oScrollView.setScrollingEnabled(true);
			}
		} else {
			Log.e(TAG, "scroll view not lockable!");
		}
	}

	final static int ROTATE_THRESHOLD = 40;
	final static int STRAIGHT_THRESHOLD = 2;
	final static int DIRECTION_THRESHOLD_1 = 10;
	final static int DIRECTION_THRESHOLD_2 = 30;
	
	@Override
	public void onUpdate(double i_dblPercentage, double i_dblAngle) {
		
		if (i_dblPercentage == 0 && i_dblAngle == 0) {
			// if percentage and angle is 0 this means the joystick was released
			// so we stop the robot
			onMove(Move.NONE, 0, 0);
			lastMove = Move.NONE;
		} else {

			// only allow a rate of updateFrequency to send drive commands
			// otherwise it will overload the robot's command queue
			if ((SystemClock.uptimeMillis() - lastTime) < 1/updateFrequency * 1000)
				return;

			lastTime = SystemClock.uptimeMillis();
			double dblAbsAngle = Math.abs(i_dblAngle);
			
			// determine which move should be executed based on the
			// last move and the angle of the joystick
			Move thisMove = Move.NONE;
			switch(lastMove) {
			case NONE:
				// for a low percentage (close to the center of the joystick) the
				// angle is too sensitive, so we only start once the percentage
				// is over the threshold
				if (i_dblPercentage < threshold) {
					return;
				}
			case ROTATE_LEFT:
			case ROTATE_RIGHT:
				// if the last move was left (or right respectively) we use a window
				// of +- 30 degrees, otherwise we switch to moving forward or backward
				// depending on the angle
				if (dblAbsAngle < ROTATE_THRESHOLD) {
					thisMove = Move.ROTATE_RIGHT;
				} else if ((180 - dblAbsAngle) < ROTATE_THRESHOLD) {
					thisMove = Move.ROTATE_LEFT;
				} else if (i_dblAngle > 0) {
					thisMove = Move.FORWARD;
				} else if (i_dblAngle < 0) {
					thisMove = Move.BACKWARD;
				}
				break;
			case STRAIGHT_BACKWARD:
			case BACKWARD:
				// if the last move was backward and the angle is within
				// 10 degrees of 0 or 180 degrees we still move backward
				// and cap the degree to 0 or 180 respectively
				// if the angle is within 30 degree we rotate on the spot
				// otherwise we change direction
				if (Utils.inInterval(i_dblAngle, -90, STRAIGHT_THRESHOLD)) {
					thisMove = Move.STRAIGHT_BACKWARD;
				} else if (i_dblAngle < 0) {
					thisMove = Move.BACKWARD;
				} else if (i_dblAngle < DIRECTION_THRESHOLD_1) {
					dblAbsAngle = 0;
					thisMove = Move.BACKWARD;
				} else if (i_dblAngle > 180 - DIRECTION_THRESHOLD_1) {
					dblAbsAngle = 180;
					thisMove = Move.BACKWARD;
				} else if (i_dblAngle < DIRECTION_THRESHOLD_2) {
					thisMove = Move.ROTATE_RIGHT;
				} else if (i_dblAngle > 180 - DIRECTION_THRESHOLD_2) {
					thisMove = Move.ROTATE_LEFT;
				} else {
					thisMove = Move.FORWARD;
				}
				break;
			case STRAIGHT_FORWARD:
			case FORWARD:
				// if the last move was forward and the angle is within
				// 10 degrees of 0 or 180 degrees we still move forward
				// and cap the degree to 0 or 180 respectively
				// if the angle is within 30 degree we rotate on the spot
				// otherwise we change direction
				if (Utils.inInterval(i_dblAngle, 90, STRAIGHT_THRESHOLD)) {
					thisMove = Move.STRAIGHT_FORWARD;
				} else if (i_dblAngle > 0) {
					thisMove = Move.FORWARD;
				} else if (i_dblAngle > -DIRECTION_THRESHOLD_1) {
					dblAbsAngle = 0;
					thisMove = Move.FORWARD;
				} else if (i_dblAngle < -(180 - DIRECTION_THRESHOLD_1)) {
					dblAbsAngle = 180;
					thisMove = Move.FORWARD;
				} else if (i_dblAngle > -DIRECTION_THRESHOLD_2) {
					thisMove = Move.ROTATE_RIGHT;
				} else if (i_dblAngle < -(180 - DIRECTION_THRESHOLD_2)) {
					thisMove = Move.ROTATE_LEFT;
				} else {
					thisMove = Move.BACKWARD;
				}
				break;
			}

			// modify the angle so that it is between -90 and +90
			// instead of 0 and 180
			// where -90 is left and +90 is right
			dblAbsAngle -= 90.0;
			
			onMove(thisMove, i_dblPercentage, dblAbsAngle);
			lastMove = thisMove;
		}
	}

	public void onMove(Move i_oMove, double i_dblSpeed, double i_dblAngle) {
		m_oRemoteControlListener.onMove(i_oMove, i_dblSpeed, i_dblAngle);
	}

	public void onMove(Move i_oMove) {
		m_oRemoteControlListener.onMove(i_oMove);
	}

	public void enableControl(boolean i_bEnable) {
		m_oRemoteControlListener.enableControl(i_bEnable);
	}
	
	public boolean isControlEnabled() {
		return m_bControl;
	}

	public void toggleAdvancedControl() {
		m_bAdvancedControl = !m_bAdvancedControl;
	}
	
	public void setAdvancedControl(boolean i_bAdvancedControl) {
//		m_bAdvancedControl = i_bAdvancedControl;
//		Utils.showLayout(m_oAdvancedControl, m_bAdvancedControl);
	}

	public boolean isAdvancedControl() {
		return m_bAdvancedControl;
	}
		
}
