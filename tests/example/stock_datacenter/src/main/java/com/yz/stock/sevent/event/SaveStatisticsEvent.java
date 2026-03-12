package com.yz.stock.sevent.event;

import com.stock.common.model.snn.EConst;
import com.stock.common.model.snn.Statistics;
import com.yz.mycore.msg.event.IEvent;

public class SaveStatisticsEvent implements IEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7918734386826017086L;
	private Statistics s;
	private long time = System.currentTimeMillis();
	public long getTime()
	{
		return time;
	}
	public SaveStatisticsEvent() {

	}

	public SaveStatisticsEvent(Statistics s) {
		this.s = s;

	}

	public int getHType() {
		// TODO Auto-generated method stub
		return EConst.EVENT_2;
	}

	public Statistics getS() {
		return s;
	}

	public void setS(Statistics s) {
		this.s = s;
	}

	public String toUpdateSql() {
		StringBuilder sb = new StringBuilder();
		sb.append("replace into t_statistics (eid, generation, times, sdesc)");
		sb.append(" values('");
		sb.append(this.getS().getEid());
		sb.append("','");
		sb.append(this.getS().getGeneration());
		sb.append("',");
		sb.append(this.getS().getHtimes());
		sb.append(",");
		sb.append(this.getS().getDesc());
		sb.append(")");

		return sb.toString();
	}

	public String getKey() {
		// TODO Auto-generated method stub
		return s.getKey();
	}

	public void setHType(int type) {
		// TODO Auto-generated method stub
		
	}

	
	
	
	
}
