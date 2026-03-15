package com.yfzx.service.db.user;

import java.util.Calendar;
import java.util.Date;

import org.jfree.util.Log;

import com.stock.common.util.CipherUtil;
import com.yz.common.vo.BaseVO;

public class ResetMemberModel extends BaseVO{

	private static final long serialVersionUID = 5399896669244549671L;
	private static final String Interferenc="盈富在线";
	private long uid;
	private String email;
	private String token;
	private Date startTime;
	private Date overTime;
	private String emailText;//邮件正文
	private Object[] other; //其他信息
	
	public String getEmailText() {
		return emailText;
	}
	public void setEmailText(String emailText) {
		this.emailText = emailText;
	}
	public Object[] getOther() {
		return other;
	}
	public void setOther(Object[] other) {
		this.other = other;
	}
	
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	public Date getStartTime() {
		return startTime;
	}
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	public Date getOverTime() {
		return overTime;
	}
	public void setOverTime(Date overTime) {
		this.overTime = overTime;
	}
	
	
	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return String.valueOf(uid);
	}
	@Override
	public String getDataType() {
		// TODO Auto-generated method stub
		return "ResetMemberModel";
	}
	
	public void setForResetMember(long uid,String email){
		this.uid=uid;
		this.email=email;
		this.startTime=new Date();  
		Calendar cal=Calendar.getInstance();
		cal.set(Calendar.DATE, cal.get(Calendar.DATE)+1);
		this.overTime=cal.getTime();
		this.token=CipherUtil.generatePassword(uid+","+email+","+startTime+","+Interferenc);
		Log.info(uid+"-->token:"+token);
		//清除操作
		cal.clear();
		cal=null;
	}
	
}
