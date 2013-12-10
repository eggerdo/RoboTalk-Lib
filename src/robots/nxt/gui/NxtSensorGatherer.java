package robots.nxt.gui;

import java.util.EnumMap;

import org.dobots.R;
import org.dobots.lib.comm.msg.ISensorDataListener;
import org.dobots.lib.comm.msg.SensorMessageArray;
import org.dobots.utilities.BaseActivity;
import org.dobots.utilities.Utils;
import org.dobots.zmq.ZmqHandler;
import org.dobots.zmq.sensors.ZmqSensorsReceiver;
import org.zeromq.ZMQ.Socket;

import robots.gui.SensorGatherer;
import robots.nxt.ctrl.LCPMessage;
import robots.nxt.ctrl.Nxt;
import robots.nxt.ctrl.NxtTypes;
import robots.nxt.ctrl.NxtTypes.DistanceData;
import robots.nxt.ctrl.NxtTypes.ENXTMotorID;
import robots.nxt.ctrl.NxtTypes.ENXTMotorSensorType;
import robots.nxt.ctrl.NxtTypes.ENXTSensorID;
import robots.nxt.ctrl.NxtTypes.ENXTSensorType;
import robots.nxt.ctrl.NxtTypes.MotorData;
import robots.nxt.ctrl.NxtTypes.SensorData;
import android.os.Looper;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TextView;

public class NxtSensorGatherer extends SensorGatherer implements ISensorDataListener {

	private static final String TAG = "NxtSensorGatherer";
	
	private Nxt m_oNxt;
	
	private boolean m_bDebug;
	
	private EnumMap<ENXTSensorID, ENXTSensorType> m_oSensorTypes;
	private EnumMap<ENXTMotorID, ENXTMotorSensorType> m_oMotorSensorTypes;

	private Socket m_oSensorsRecvSocket;

	private ZmqSensorsReceiver m_oSensorsReceiver;
	
	public NxtSensorGatherer(BaseActivity i_oActivity, Nxt i_oNxt) {
		super(i_oActivity, "NxtSensorGatherer");
		m_oNxt = i_oNxt;
		
		m_oSensorTypes = new EnumMap<ENXTSensorID, ENXTSensorType>(ENXTSensorID.class);
		m_oMotorSensorTypes = new EnumMap<NxtTypes.ENXTMotorID, NxtTypes.ENXTMotorSensorType>(ENXTMotorID.class);
		
		m_oSensorsRecvSocket = ZmqHandler.getInstance().obtainSensorsRecvSocket();
		m_oSensorsRecvSocket.subscribe(m_oNxt.getID().getBytes());
		m_oSensorsReceiver = new ZmqSensorsReceiver(m_oSensorsRecvSocket, "NxtSensorsReceiver");
		m_oSensorsReceiver.setSensorDataListener(this);
		m_oSensorsReceiver.start();
		
		// set up the maps
		initialize();
	}
		
	public void initialize() {
		// set up the maps
		for (ENXTSensorID sensor : ENXTSensorID.values()) {
			m_oSensorTypes.put(sensor, ENXTSensorType.sensType_None);
		}
		
		for (ENXTMotorID motor : ENXTMotorID.values()) {
			m_oMotorSensorTypes.put(motor, ENXTMotorSensorType.motor_degreee);
		}
	}
	
	public void setSensorType(ENXTSensorID i_eSensor, ENXTSensorType i_eSensorType) {
		if (m_oNxt.isConnected()) {
			m_oNxt.setSensorType(i_eSensor, i_eSensorType);
			m_oSensorTypes.put(i_eSensor, i_eSensorType);
		}
	}
	
	public void enableSensor(ENXTSensorID i_eSensor, boolean i_bEnabled) {
		if (m_oNxt.isConnected()) {
			if (i_bEnabled) {
				m_oNxt.startSensorDataStreaming(i_eSensor, m_oSensorTypes.get(i_eSensor));
			} else {
				m_oNxt.stopSensorDataStreaming(i_eSensor);
			}
		}
		
		showSensor(i_eSensor, i_bEnabled);
	}

	public void setMotorSensorType(ENXTMotorID i_eMotor,
			ENXTMotorSensorType eMotorSensorType) {
		m_oMotorSensorTypes.put(i_eMotor, eMotorSensorType);
	}
	
	public void enableMotor(ENXTMotorID i_eMotor, boolean i_bEnabled) {
		if (m_oNxt.isConnected()) {
			if (i_bEnabled) {
				m_oNxt.startMotorDataStreaming(i_eMotor);
			} else {
				m_oNxt.stopMotorDataStreaming(i_eMotor);
			}
		}
		
		showMotor(i_eMotor, i_bEnabled);
	}

	@Override
	public void onSensorData(String json) {
		// to update the UI we need to execute the function
		// in the main looper.
		final SensorMessageArray data = SensorMessageArray.decodeJSON(m_oNxt.getID(), json);
		
		m_oActivity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				if (data.getSensorID().equals("sensor_data")) {
					updateGUI(NxtTypes.assembleSensorData(data));
				} else if (data.getSensorID().equals("motor_data")) {
					updateGUI(NxtTypes.assembleMotorData(data));
				} else if (data.getSensorID().equals("distance_data")) {
					updateGUI(NxtTypes.assembleDistanceData(data));
				}
			}
		});
	}
	
	private void updateGUI(MotorData i_oMotorData) {
		int nOutputPort = i_oMotorData.nOutputPort;
		Log.d(TAG, "update " + nOutputPort);

		ENXTMotorID eMotor;
		int nResPowerSetpointID, nResTachoCountID, nResRotationCountID;
		
		int nInvertedFactor = m_oNxt.isInverted() ? -1 : 1;
		
		switch (nOutputPort) {
		case 0:
			eMotor = ENXTMotorID.motor_1;
			nResPowerSetpointID = R.id.txtMotor1PowerSetpoint;
			nResTachoCountID 	= R.id.txtMotor1TachoCount;
//			nResBlockTachoCountID = R.id.txtMotor1BlockTachoCount;
			nResRotationCountID = R.id.txtMotor1RotationCount;
			break;
		case 1:
			eMotor = ENXTMotorID.motor_2;
			nResPowerSetpointID = R.id.txtMotor2PowerSetpoint;
			nResTachoCountID 	= R.id.txtMotor2TachoCount;
//			nResBlockTachoCountID = R.id.txtMotor2BlockTachoCount;
			nResRotationCountID = R.id.txtMotor2RotationCount;
			break;
		case 2:
			eMotor = ENXTMotorID.motor_3;
			nResPowerSetpointID = R.id.txtMotor3PowerSetpoint;
			nResTachoCountID 	= R.id.txtMotor3TachoCount;
//			nResBlockTachoCountID = R.id.txtMotor3BlockTachoCount;
			nResRotationCountID = R.id.txtMotor3RotationCount;
			break;
		default:
			return;
		}
		
    	TextView txtPowerSetpoint = (TextView) m_oActivity.findViewById(nResPowerSetpointID);
    	txtPowerSetpoint.setText(String.valueOf(i_oMotorData.nPowerSetpoint));

    	TextView txtRotationCount = (TextView) m_oActivity.findViewById(nResRotationCountID);
    	if (m_oMotorSensorTypes.get(eMotor) == ENXTMotorSensorType.motor_degreee) {
	    	txtRotationCount.setText(String.valueOf(nInvertedFactor * i_oMotorData.nRotationCount));
    	} else {
	    	txtRotationCount.setText(String.format("%.2f", nInvertedFactor * i_oMotorData.nRotationCount / 360.0));
    	}

    	if (m_bDebug) {
	    	TextView txtTachoCount = (TextView) m_oActivity.findViewById(nResTachoCountID);
	    	txtTachoCount.setText(String.valueOf(nInvertedFactor * i_oMotorData.nTachoCount));
    	}

//		m_oMotorRequestActive.put(eMotor, false);
	}
	
	private void updateGUI(DistanceData i_oDistanceData) {
		int nInputPort = i_oDistanceData.nInputPort;
		Log.d(TAG, "update " + nInputPort);

		ENXTSensorID eSensor;
    	int nResRawID, nResNormID, nResScaleID, nResCalibID;
    	
    	// get resource id based on sensor id
    	switch (nInputPort) {
    	case 0:
    		eSensor = ENXTSensorID.sens_sensor1;
    		nResScaleID = R.id.txtSensor1ScaleValue;
    		nResRawID 	= R.id.txtSensor1RawValue;
    		nResNormID 	= R.id.txtSensor1NormValue;
    		nResCalibID = R.id.txtSensor1CalibValue;
    		break;
    	case 1:
    		eSensor = ENXTSensorID.sens_sensor2;
    		nResRawID 	= R.id.txtSensor2RawValue;
    		nResNormID 	= R.id.txtSensor2NormValue;
    		nResScaleID = R.id.txtSensor2ScaleValue;
    		nResCalibID = R.id.txtSensor2CalibValue;
    		break;
    	case 2:
    		eSensor = ENXTSensorID.sens_sensor3;
    		nResRawID 	= R.id.txtSensor3RawValue;
    		nResNormID 	= R.id.txtSensor3NormValue;
    		nResScaleID = R.id.txtSensor3ScaleValue;
    		nResCalibID = R.id.txtSensor3CalibValue;
    		break;
    	case 3:
    		eSensor = ENXTSensorID.sens_sensor4;
    		nResRawID 	= R.id.txtSensor4RawValue;
    		nResNormID 	= R.id.txtSensor4NormValue;
    		nResScaleID = R.id.txtSensor4ScaleValue;
    		nResCalibID = R.id.txtSensor4CalibValue;
    		break;
		default:
			return;
    	}

    	TextView txtScaleValue = (TextView) m_oActivity.findViewById(nResScaleID);
    	
    	if (i_oDistanceData.nStatus == LCPMessage.SUCCESS) {
	    	txtScaleValue.setText(String.valueOf(i_oDistanceData.nDistance));
    	} else {
    		txtScaleValue.setText("????");
    	}

    	if (m_bDebug) {
	    	TextView txtNormValue = (TextView) m_oActivity.findViewById(nResNormID);
	    	txtNormValue.setText("-");
	
	    	TextView txtRawValue = (TextView) m_oActivity.findViewById(nResRawID);
	    	txtRawValue.setText("-");
	
	    	TextView txtCalibValue = (TextView) m_oActivity.findViewById(nResCalibID);
	    	txtCalibValue.setText("-");
    	}

//		m_oSensorRequestActive.put(eSensor, false);
	}

	private void updateGUI(SensorData i_oSensorData) {
		int nInputPort = i_oSensorData.nInputPort;
		Log.d(TAG, "update " + nInputPort);

    	int nResRawID, nResNormID, nResScaleID, nResCalibID;
    	
    	// get resource id based on sensor id
    	switch (ENXTSensorID.fromValue(nInputPort)) {
    	case sens_sensor1:
    		nResScaleID = R.id.txtSensor1ScaleValue;
    		nResRawID 	= R.id.txtSensor1RawValue;
    		nResNormID 	= R.id.txtSensor1NormValue;
    		nResCalibID = R.id.txtSensor1CalibValue;
    		break;
    	case sens_sensor2:
    		nResRawID 	= R.id.txtSensor2RawValue;
    		nResNormID 	= R.id.txtSensor2NormValue;
    		nResScaleID = R.id.txtSensor2ScaleValue;
    		nResCalibID = R.id.txtSensor2CalibValue;
    		break;
    	case sens_sensor3:
    		nResRawID 	= R.id.txtSensor3RawValue;
    		nResNormID 	= R.id.txtSensor3NormValue;
    		nResScaleID = R.id.txtSensor3ScaleValue;
    		nResCalibID = R.id.txtSensor3CalibValue;
    		break;
    	case sens_sensor4:
    		nResRawID 	= R.id.txtSensor4RawValue;
    		nResNormID 	= R.id.txtSensor4NormValue;
    		nResScaleID = R.id.txtSensor4ScaleValue;
    		nResCalibID = R.id.txtSensor4CalibValue;
    		break;
		default:
			return;
    	}
    	
    	String strScaledValue, strNormalizedValue, strCalibratedValue, strRawValue;
    	
    	if (i_oSensorData.nStatus == LCPMessage.SUCCESS) {
    		strScaledValue = String.valueOf(i_oSensorData.nScaledValue);
    		strNormalizedValue = String.valueOf(i_oSensorData.nNormalizedValue);
    		strCalibratedValue = String.valueOf(i_oSensorData.nCalibratedValue);
    		strRawValue = String.valueOf(i_oSensorData.nRawValue);
    	} else {
    		String unknown = "????";
    		strScaledValue = unknown;
    		strNormalizedValue = unknown;
    		strCalibratedValue = unknown;
    		strRawValue = unknown;
    	}

    	TextView txtScaleValue = (TextView) m_oActivity.findViewById(nResScaleID);
        txtScaleValue.setText(strScaledValue);

    	if (m_bDebug) {
	    	TextView txtNormValue = (TextView) m_oActivity.findViewById(nResNormID);
	    	txtNormValue.setText(strNormalizedValue);
	
	    	TextView txtRawValue = (TextView) m_oActivity.findViewById(nResRawID);
	    	txtRawValue.setText(strRawValue);
	
	    	TextView txtCalibValue = (TextView) m_oActivity.findViewById(nResCalibID);
	    	txtCalibValue.setText(strCalibratedValue);
    	}

//		m_oSensorRequestActive.put(eSensor, false);
	}

//	private void updateGUI(SensorData i_oSensorData) {
//		int nInputPort = 0;
//		try {
//			nInputPort = i_oSensorData.getItem("input_port").getInt();
//		} catch (JSONException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (SensorValueTypeException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//			return;
//		}
//		
//		ENXTSensorID eSensor;
//    	int nResRawID, nResNormID, nResScaleID, nResCalibID;
//    	
//    	// get resource id based on sensor id
//    	switch (nInputPort) {
//    	case 0:
//    		eSensor = ENXTSensorID.sens_sensor1;
//    		nResScaleID = R.id.txtSensor1ScaleValue;
//    		nResRawID 	= R.id.txtSensor1RawValue;
//    		nResNormID 	= R.id.txtSensor1NormValue;
//    		nResCalibID = R.id.txtSensor1CalibValue;
//    		break;
//    	case 1:
//    		eSensor = ENXTSensorID.sens_sensor2;
//    		nResRawID 	= R.id.txtSensor2RawValue;
//    		nResNormID 	= R.id.txtSensor2NormValue;
//    		nResScaleID = R.id.txtSensor2ScaleValue;
//    		nResCalibID = R.id.txtSensor2CalibValue;
//    		break;
//    	case 2:
//    		eSensor = ENXTSensorID.sens_sensor3;
//    		nResRawID 	= R.id.txtSensor3RawValue;
//    		nResNormID 	= R.id.txtSensor3NormValue;
//    		nResScaleID = R.id.txtSensor3ScaleValue;
//    		nResCalibID = R.id.txtSensor3CalibValue;
//    		break;
//    	case 3:
//    		eSensor = ENXTSensorID.sens_sensor4;
//    		nResRawID 	= R.id.txtSensor4RawValue;
//    		nResNormID 	= R.id.txtSensor4NormValue;
//    		nResScaleID = R.id.txtSensor4ScaleValue;
//    		nResCalibID = R.id.txtSensor4CalibValue;
//    		break;
//		default:
//			return;
//    	}
//    	
//    	String strScaledValue, strNormalizedValue, strCalibratedValue, strRawValue;
//    	
//		String unknown = "????";
//		strScaledValue = unknown;
//		strNormalizedValue = unknown;
//		strCalibratedValue = unknown;
//		strRawValue = unknown;
//    	try {
//			if (i_oSensorData.getItem("status").getInt() == LCPMessage.SUCCESS) {
//				strScaledValue = String.valueOf(i_oSensorData.getItem("scaled_value").getInt());
//				strNormalizedValue = String.valueOf(i_oSensorData.getItem("normalized_value").getInt());
//				strCalibratedValue = String.valueOf(i_oSensorData.getItem("calibrated_value").getInt());
//				strRawValue = String.valueOf(i_oSensorData.getItem("raw_value").getInt());
//			}
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (SensorValueTypeException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//    	TextView txtScaleValue = (TextView) m_oActivity.findViewById(nResScaleID);
//        txtScaleValue.setText(strScaledValue);
//
//    	if (m_bDebug) {
//	    	TextView txtNormValue = (TextView) m_oActivity.findViewById(nResNormID);
//	    	txtNormValue.setText(strNormalizedValue);
//	
//	    	TextView txtRawValue = (TextView) m_oActivity.findViewById(nResRawID);
//	    	txtRawValue.setText(strRawValue);
//	
//	    	TextView txtCalibValue = (TextView) m_oActivity.findViewById(nResCalibID);
//	    	txtCalibValue.setText(strCalibratedValue);
//    	}
//
////		m_oSensorRequestActive.put(eSensor, false);
//	}
	

	public void showSensor(ENXTSensorID i_eSensor, boolean i_bShow) {

    	int nResID;
    	
    	// get resource id based on sensor id
    	switch (i_eSensor) {
    	case sens_sensor1:
    		nResID = R.id.tblSensor1_data;
    		break;
    	case sens_sensor2:
    		nResID = R.id.tblSensor2_data;
    		break;
    	case sens_sensor3:
    		nResID = R.id.tblSensor3_data;
    		break;
    	case sens_sensor4:
    		nResID = R.id.tblSensor4_data;
    		break;
		default:
			return;
    	}

    	Utils.showLayout((TableLayout)m_oActivity.findViewById(nResID), i_bShow);
	}

	public void showMotor(ENXTMotorID i_eMotor, boolean i_bShow) {

    	int nResID;
    	
    	// get resource id based on sensor id
    	switch (i_eMotor) {
    	case motor_1:
    		nResID = R.id.tblMotor1_data;
    		break;
    	case motor_2:
    		nResID = R.id.tblMotor2_data;
    		break;
    	case motor_3:
    		nResID = R.id.tblMotor3_data;
    		break;
		default:
			return;
    	}
    	
    	Utils.showLayout((TableLayout)m_oActivity.findViewById(nResID), i_bShow);
	}

	public void setDebug(boolean i_bDebug) {
		m_bDebug = i_bDebug;
	}

	@Override
	public void shutDown() {
		// TODO Auto-generated method stub
		
	}

}
