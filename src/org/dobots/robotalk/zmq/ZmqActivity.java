package org.dobots.robotalk.zmq;

import android.app.Activity;

public abstract class ZmqActivity extends Activity {
	
	public abstract void ready();
	
	public abstract void failed();
	
}

