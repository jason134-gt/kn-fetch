package com.yz.stock.realtime;

import com.yz.mycore.core.plug.IPlugIn;
import com.yz.mycore.core.plug.Irefresh;

public class RealtimeFetchPlug implements IPlugIn ,Irefresh{

	public void plugIn() {
		RealDataComputeTimer.getInstance().startRealtimeFetch();
	}

	public void refresh() {
		
		
	}

	public void beforeRefresh() {
		
	}

}
