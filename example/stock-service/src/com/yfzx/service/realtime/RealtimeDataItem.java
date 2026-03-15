package com.yfzx.service.realtime;

import java.util.Date;

import com.stock.common.model.trade.StockTrade;
import com.stock.common.msg.BaseMsg;
import com.stock.common.util.DateUtil;

public class RealtimeDataItem extends BaseMsg{

	/**
	 * 
	 */
	private static final long serialVersionUID = 2535514198955859451L;
	private String uidentify;
	private String name;
	double l;//最低价
	double h;//最高价
	double c;//当前价
	double zs;//昨是收盘价
	double jk;//今日开盘价
	double cjl;//成交量
	double cje;//成交额
	double czf;//今日涨幅
	double cjeAdd;//成交额
	double cjlAdd;//今日涨幅
	int zhnl;//综合能力评分
	long uptime;
	private float fbp1 ;         // 委托买价1
	private float fbp2 ;         // 委托买价2
	private float fbp3 ;         // 委托买价3
	private float fbp4 ;         // 委托买价4
	private float fbp5 ;         // 委托买价5
	private float fbv1 ;         // 委托买量1
	private float fbv2 ;        // 委托买量2
	private float fbv3 ;        // 委托买量3
	private float fbv4 ;        // 委托买量4w
	private float fbv5 ;        // 委托买量5
	private float fsp1 ;        // 委托卖价1
	private float fsp2 ;        // 委托卖价2
	private float fsp3 ;        // 委托卖价3
	private float fsp4 ;        // 委托卖价4
	private float fsp5 ;        // 委托卖价5
	private float fsv1 ;        // 委托卖量1
	private float fsv2 ;        // 委托卖量2
	private float fsv3 ;        // 委托卖量3
	private float fsv4 ;        // 委托卖量4
	private float fsv5 ;        // 委托卖量5
	public boolean isindustry = false;
	public RealtimeDataItem(StockTrade st) {
		this.setC(st.getC());
		this.setCje(st.getCje());
		this.setCjl(st.getCjl());
		this.setUidentify(st.getCode());
		this.setCzf(st.getCzf());
		this.setH(st.getH());
		this.setJk(st.getJk());
		this.setL(st.getL());
		this.setName(st.getName());
		this.setUptime(st.getUptime());
		this.setZs(st.getZs());
		this.setTime(st.getUptime());
		this.setFbp1(st.getFbp1());
		this.setFbp2(st.getFbp2());
		this.setFbp3(st.getFbp3());
		this.setFbp4(st.getFbp4());
		this.setFbp5(st.getFbp5());
		this.setFbv1(st.getFbv1());
		this.setFbv2(st.getFbv2());
		this.setFbv3(st.getFbv3());
		this.setFbv4(st.getFbv4());
		this.setFbv5(st.getFbv5());
		this.setFsp1(st.getFsp1());
		this.setFsp2(st.getFsp2());
		this.setFsp3(st.getFsp3());
		this.setFsp4(st.getFsp4());
		this.setFsp5(st.getFsp5());
		this.setFsv1(st.getFsv1());
		this.setFsv2(st.getFsv2());
		this.setFsv3(st.getFsv3());
		this.setFsv4(st.getFsv4());
		this.setFsv5(st.getFsv5());
	}
	
	public String getUidentify() {
		return uidentify;
	}

	public void setUidentify(String uidentify) {
		this.uidentify = uidentify;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public double getL() {
		return l;
	}
	public void setL(double l) {
		this.l = l;
	}
	public double getH() {
		return h;
	}
	public void setH(double h) {
		this.h = h;
	}
	public double getC() {
		return c;
	}
	public void setC(double c) {
		this.c = c;
	}
	public double getZs() {
		return zs;
	}
	public void setZs(double zs) {
		this.zs = zs;
	}
	public double getJk() {
		return jk;
	}
	public void setJk(double jk) {
		this.jk = jk;
	}
	public double getCjl() {
		return cjl;
	}
	public void setCjl(double cjl) {
		this.cjl = cjl;
	}
	public double getCje() {
		return cje;
	}
	public void setCje(double cje) {
		this.cje = cje;
	}
	public double getCzf() {
		if(getJk()==0)
			return 0.0;
		return (getC() - getJk())/getJk()*100;
	}
	public void setCzf(double czf) {
		this.czf = czf;
	}
	public int getZhnl() {
		return zhnl;
	}
	public void setZhnl(int zhnl) {
		this.zhnl = zhnl;
	}
	public long getUptime() {
		return uptime;
	}
	public void setUptime(long uptime) {
		this.uptime = uptime;
	}
	public float getFbp1() {
		return fbp1;
	}
	public void setFbp1(float fbp1) {
		this.fbp1 = fbp1;
	}
	public float getFbp2() {
		return fbp2;
	}
	public void setFbp2(float fbp2) {
		this.fbp2 = fbp2;
	}
	public float getFbp3() {
		return fbp3;
	}
	public void setFbp3(float fbp3) {
		this.fbp3 = fbp3;
	}
	public float getFbp4() {
		return fbp4;
	}
	public void setFbp4(float fbp4) {
		this.fbp4 = fbp4;
	}
	public float getFbp5() {
		return fbp5;
	}
	public void setFbp5(float fbp5) {
		this.fbp5 = fbp5;
	}
	public float getFbv1() {
		return fbv1;
	}
	public void setFbv1(float fbv1) {
		this.fbv1 = fbv1;
	}
	public float getFbv2() {
		return fbv2;
	}
	public void setFbv2(float fbv2) {
		this.fbv2 = fbv2;
	}
	public float getFbv3() {
		return fbv3;
	}
	public void setFbv3(float fbv3) {
		this.fbv3 = fbv3;
	}
	public float getFbv4() {
		return fbv4;
	}
	public void setFbv4(float fbv4) {
		this.fbv4 = fbv4;
	}
	public float getFbv5() {
		return fbv5;
	}
	public void setFbv5(float fbv5) {
		this.fbv5 = fbv5;
	}
	public float getFsp1() {
		return fsp1;
	}
	public void setFsp1(float fsp1) {
		this.fsp1 = fsp1;
	}
	public float getFsp2() {
		return fsp2;
	}
	public void setFsp2(float fsp2) {
		this.fsp2 = fsp2;
	}
	public float getFsp3() {
		return fsp3;
	}
	public void setFsp3(float fsp3) {
		this.fsp3 = fsp3;
	}
	public float getFsp4() {
		return fsp4;
	}
	public void setFsp4(float fsp4) {
		this.fsp4 = fsp4;
	}
	public float getFsp5() {
		return fsp5;
	}
	public void setFsp5(float fsp5) {
		this.fsp5 = fsp5;
	}
	public float getFsv1() {
		return fsv1;
	}
	public void setFsv1(float fsv1) {
		this.fsv1 = fsv1;
	}
	public float getFsv2() {
		return fsv2;
	}
	public void setFsv2(float fsv2) {
		this.fsv2 = fsv2;
	}
	public float getFsv3() {
		return fsv3;
	}
	public void setFsv3(float fsv3) {
		this.fsv3 = fsv3;
	}
	public float getFsv4() {
		return fsv4;
	}
	public void setFsv4(float fsv4) {
		this.fsv4 = fsv4;
	}
	public float getFsv5() {
		return fsv5;
	}
	public void setFsv5(float fsv5) {
		this.fsv5 = fsv5;
	}
	public boolean isIsindustry() {
		return isindustry;
	}
	public void setIsindustry(boolean isindustry) {
		this.isindustry = isindustry;
	}
	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return this.getUidentify();
	}

	public double getCjeAdd() {
		if(cjeAdd==0)
			cjeAdd = cje;
		return cjeAdd;
	}

	public void setCjeAdd(double cjeAdd) {
		this.cjeAdd = cjeAdd;
	}


	public double getCjlAdd() {
		if(cjlAdd==0)
			cjlAdd = cjl;
		return cjlAdd;
	}

	public void setCjlAdd(double cjlAdd) {
		this.cjlAdd = cjlAdd;
	}

	@Override
	public String toString() {
		return "RealtimeDataItem [uidentify=" + uidentify + ", name=" + name
				+ ", l=" + l + ", h=" + h + ", c=" + c + ", zs=" + zs + ", jk="
				+ jk + ", cjl=" + cjl + ", cje=" + cje + ", czf=" + getCzf()
				+ ", cjlAdd=" + cjlAdd + ", cjeAdd=" + cjeAdd 
				+ ", zhnl=" + zhnl + ", uptime=" + DateUtil.format2String(new Date(uptime)) + ", fbp1=" + fbp1
				+ ", fbp2=" + fbp2 + ", fbp3=" + fbp3 + ", fbp4=" + fbp4
				+ ", fbp5=" + fbp5 + ", fbv1=" + fbv1 + ", fbv2=" + fbv2
				+ ", fbv3=" + fbv3 + ", fbv4=" + fbv4 + ", fbv5=" + fbv5
				+ ", fsp1=" + fsp1 + ", fsp2=" + fsp2 + ", fsp3=" + fsp3
				+ ", fsp4=" + fsp4 + ", fsp5=" + fsp5 + ", fsv1=" + fsv1
				+ ", fsv2=" + fsv2 + ", fsv3=" + fsv3 + ", fsv4=" + fsv4
				+ ", fsv5=" + fsv5 + "]";
	}


	
	

	
}
