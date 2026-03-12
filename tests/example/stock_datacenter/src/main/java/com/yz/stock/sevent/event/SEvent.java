package com.yz.stock.sevent.event;

import java.util.Date;

import com.stock.common.model.snn.EConst;
import com.stock.common.model.snn.Gene;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yz.mycore.msg.event.IEvent;

public class SEvent implements IEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5320535114860603033L;
	private Gene g;
	private String sourceid;
	private Date stime;
	private Date etime;
	private String eid;
	private boolean isSave = false;
	private long time = System.currentTimeMillis();
	public long getTime()
	{
		return time;
	}
	public SEvent() {

	}

	public SEvent(Gene g, Date stime, Date etime) {
		this.g = g;
		this.stime = stime;
		this.etime = etime;
		this.eid = g.getKey();
	}

	public Gene getG() {
		return g;
	}

	public void setG(Gene g) {
		this.g = g;
	}

	public Date getStime() {
		return stime;
	}

	public void setStime(Date stime) {
		this.stime = stime;
	}

	public Date getEtime() {
		return etime;
	}

	public void setEtime(Date etime) {
		this.etime = etime;
	}

	public String getSourceid() {
		return sourceid;
	}

	public void setSourceid(String sourceid) {
		this.sourceid = sourceid;
	}

	public String getEid() {
		if (StringUtil.isEmpty(eid) && g != null)
			return g.getKey();
		return eid;
	}

	public void setEid(String gid) {
		this.eid = gid;
	}

	public int getHType() {
		// TODO Auto-generated method stub
		return EConst.EVENT_0;
	}

	public boolean isSave() {
		return isSave;
	}

	public void setSave(boolean isSave) {
		this.isSave = isSave;
	}

	public String toUpdateSql() {

		String tablename = StockUtil.getSEventTableName(sourceid);
		StringBuilder sb = new StringBuilder();
		sb.append("replace into ");
		sb.append(tablename);
		sb.append("(eid,sourceid,stime,etime,uptime) ");
		sb.append(" values('");
		sb.append(this.getEid());
		sb.append("','");
		sb.append(this.sourceid);
		sb.append("',FROM_UNIXTIME(");
		sb.append(stime.getTime() / 1000);
		sb.append(")");
		sb.append(",FROM_UNIXTIME(");
		sb.append(etime.getTime() / 1000);
		sb.append("),");
		sb.append("now()");
		sb.append(")");

		return sb.toString();
	}

	@Override
	public String toString() {
		return "SEvent [g=" + g + ", sourceid=" + sourceid + ", stime=" + stime
				+ ", etime=" + etime + ", eid=" + eid + "]";
	}

	public String getKey() {
		// TODO Auto-generated method stub
		return this.sourceid+"^"+this.eid+"^"+this.stime.getTime()+"^"+this.etime;
	}

	public void setHType(int type) {
		// TODO Auto-generated method stub
		
	}
	
	
}
