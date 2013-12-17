package robots.arduino.ctrl.remote;

import robots.RobotType;
import robots.arduino.ctrl.IArduino;
import robots.gui.RobotView;
import robots.gui.comm.IRobotConnection;
import robots.remote.RemoteRobotBinder;

public class ArduinoRemoteBinder extends RemoteRobotBinder implements IArduino {

	public ArduinoRemoteBinder(RobotView activity, Class serviceClass) {
		super(activity, serviceClass);
	}
	
	protected IArduino getArduino() {
		return (IArduino)mRobot;
	}

	@Override
	public RobotType getType() {
		return RobotType.RBT_ARDUINO;
	}

	@Override
	public IRobotConnection getConnection() {
		if (mBound) {
			return getArduino().getConnection();
		} else {
			return null;
		}
	}

	@Override
	public void setConnection(IRobotConnection connection) {
		if (mBound) {
			getArduino().setConnection(connection);
		}
	}

	@Override
	public void requestSensorData() {
		if (mBound) {
			getArduino().requestSensorData();
		}
	}

}
