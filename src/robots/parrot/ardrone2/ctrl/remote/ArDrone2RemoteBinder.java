package robots.parrot.ardrone2.ctrl.remote;

import robots.gui.RobotView;
import robots.parrot.ardrone2.ctrl.IArDrone2;
import robots.parrot.ctrl.remote.ParrotRemoteBinder;
import android.widget.ImageView;

public class ArDrone2RemoteBinder extends ParrotRemoteBinder implements IArDrone2 {

	public ArDrone2RemoteBinder(RobotView activity, Class serviceClass) {
		super(activity, serviceClass);
	}

	@Override
	public void setVideoView(ImageView view) {
		if (mBound) {
			((IArDrone2)getParrot()).setVideoView(view);
		}
	}

}