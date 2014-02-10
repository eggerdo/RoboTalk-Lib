/**
* 456789------------------------------------------------------------------------------------------------------------120
*
* @brief:	Helper class to handle remote control
* @file: ${file_name}
*
* @desc:	Creates messsages out of remote commands and sends them out.
*
*
* Copyright (c) 2013 Dominik Egger <dominik@dobots.nl>
*
* @author:		Dominik Egger
* @date:		2.9.2013
* @project:		Robot-Lib
* @company:		Distributed Organisms B.V.
*/
package robots.ctrl.control;

import org.dobots.comm.Move;
import org.dobots.comm.msg.RoboCommandTypes.CameraCommandType;
import org.dobots.comm.msg.RoboCommands;
import org.dobots.comm.msg.RoboCommands.BaseCommand;
import org.dobots.comm.msg.RoboCommands.CameraCommand;
import org.dobots.comm.msg.RoboCommands.ControlCommand;
import org.dobots.comm.msg.RoboCommands.DriveCommand;


public abstract class RemoteControlSender implements IDriveControlListener, ICameraControlListener {

	private static final int BASE_SPEED = -1;
	
	protected String m_strRobot = "";
	
	public void setRobotId(String i_strRobot) {
		m_strRobot = i_strRobot;
	}

	@Override
	// callback function when the joystick is used
	public void onMove(Move i_oMove, double i_dblSpeed, double i_dblAngle) {
		sendMove(i_oMove, i_dblSpeed, i_dblAngle);
	}
	
	@Override
	// callback function when the arrow keys are used
	public void onMove(Move i_oMove) {
		// execute this move
		sendMove(i_oMove, BASE_SPEED, 0.0);
	}

	protected void sendMove(Move i_eMove, double i_dblSpeed, double i_dblAngle) {
		DriveCommand oCmd = RoboCommands.createDriveCommand(m_strRobot, i_eMove, i_dblSpeed, i_dblAngle);
		sendCommand(oCmd);
	}
	
	protected void sendCameraCommand(CameraCommandType i_eType) {
		CameraCommand oCmd = RoboCommands.createCameraCommand(m_strRobot, i_eType);
		sendCommand(oCmd);
	}

	public void toggleCamera() {
		sendCameraCommand(CameraCommandType.TOGGLE);
	}
	
	public void startVideo() {
		sendCameraCommand(CameraCommandType.ON);
	}

	public void stopVideo() {
		sendCameraCommand(CameraCommandType.OFF);
	}

	@Override
	public void cameraUp() {
		sendCameraCommand(CameraCommandType.UP);
	}

	@Override
	public void cameraDown() {
		sendCameraCommand(CameraCommandType.DOWN);
	}

	@Override
	public void cameraStop() {
		sendCameraCommand(CameraCommandType.STOP);
	}
	
	protected abstract void sendCommand(BaseCommand i_oCmd);

	@Override
	public void enableControl(boolean i_bEnable) {
		ControlCommand oCmd = RoboCommands.createControlCommand(m_strRobot, "enableControl", i_bEnable);
		sendCommand(oCmd);
	}
	
	public void toggleInvertDrive() {
		
	}

	public abstract void close();
	
}
