package com.yz.stock.realtime;

import com.yz.mycore.core.plug.IPlugIn;
import com.yz.mycore.core.plug.Irefresh;

public class RealtimeComputePlug implements IPlugIn ,Irefresh{

	public void plugIn() {
		RealDataComputeTimer.getInstance().startRealCompute();
	}

	public void refresh() {
		
		
	}

	public void beforeRefresh() {
		
	}

}
