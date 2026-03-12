package com.yfzx.service.share;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import com.stock.common.model.share.TimeLine;
import com.stock.common.msg.MsgConst;
import com.stock.common.util.CassandraHectorGateWay;

/**
 * 关于消息的时间轴 <br>
 * 用户持久化和查询<br>
 * 存储结构如：
 * ColunFamily|-[key]|--SuperColumn[sn]<br>
 * 			  |		 |  |--Column_1[name]--[value]<br>
 * 			  |		 |  |--Column_...<br>
 * 			  |		 | 
 *            |      |--SuperColumn[sn2]<br>
 * 			  |		    |--Column_1[name]--[value]<br>
 * 			  |		    |--Column_...<br>
 * 			  |		    
 * 			  |-[key2]|--SuperColumn[sn...]<br>	
 * 例子 9给18发了一条消息，其中key=9,sn=18,column_1[uuid=xxxx]  <br>
 * at消息，对话消息，评论消息也存储在这里，article消息不需要存储
 */
public class MsgTimeLineService {

	private static ReentrantLock reentrantLock = new ReentrantLock(true);//公平锁
	private static final String TABLE_TIMELINE = "MsgTimeLine";
	public static final String TABLE_SVAE = "message";
	private static CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
	
	private static MsgTimeLineService instance = new MsgTimeLineService();
	
	public static MsgTimeLineService getInstance()
	{
		return instance;
	}
	private static HashMap<Integer,String> typeCache ;
	
	//
	private static enum MsgConstType {
		MSG_USER_TYPE_0(MsgConst.MSG_USER_TYPE_0,"publishArticle")//发表博文消息
		,MSG_USER_TYPE_1(MsgConst.MSG_USER_TYPE_1,"reprinted")//转载
		,MSG_USER_TYPE_2(MsgConst.MSG_USER_TYPE_2,"liked")//赞
		,MSG_USER_TYPE_3(MsgConst.MSG_USER_TYPE_3,"atArticle")//@我的博文
		,MSG_USER_TYPE_4(MsgConst.MSG_USER_TYPE_4,"receiveComment")//评论[我收到的评论]
		,MSG_USER_TYPE_5(MsgConst.MSG_USER_TYPE_5,"follow")//关注
		,MSG_USER_TYPE_6(MsgConst.MSG_USER_TYPE_6,"unfollow")//取消关注
		,MSG_USER_TYPE_7(MsgConst.MSG_USER_TYPE_7,"talk")//取消关注
		,MSG_USER_TYPE_13(MsgConst.MSG_USER_TYPE_13,"atComment")//@我的评论
		,MSG_USER_TYPE_14(MsgConst.MSG_USER_TYPE_14,"sendComment")//我发出的评论
		;
		private int k;
		private String v;
		  
		MsgConstType(int k ,String v){
			this.k = k;
			this.v = v;
		}
		public int getK(){
			return k;
		}
		public String getV(){
			return v;
		}
		public String toString(){
			return this.v;
		}	
		
	};
	
	public static String reSn(int msgConstType){
		if(typeCache == null){
			try{
				reentrantLock.lock();
				typeCache = new HashMap<Integer,String>();
				for (MsgConstType mct : MsgConstType.values()) {
					typeCache.put(mct.getK(), mct.getV());
				}
			}finally{
				reentrantLock.unlock();
			}
		}
		return typeCache.get(msgConstType);
	}
	
	public void saveTimeLine(String key,String uuid,String sn,long timemillis){
		TimeLine tl = new TimeLine(String.valueOf(timemillis), uuid);
		ch.insertTimeLine(TABLE_TIMELINE,key,sn,tl);
	}
	
	public void deltetTimeLine(String key,String sn,long timemillis){
		ch.deltetNameInSuperColumn(TABLE_TIMELINE, key,sn, String.valueOf(timemillis));
	}
	//如删除时光轴上的文章 [文章里有评论列表]
	public void deltetTimeLine(String key,String sn){
		ch.deleteSuperColumn(TABLE_TIMELINE, key,sn);
	}
	
	/**
	 * 此类接口 用于按时间范围分页 比如查询24小时的文章
	 */
	public List<TimeLine> getTimeLineListByTime(String key,String sn ,long startTime,long endTime){		
		List<TimeLine> timeLineList = ch.getTimeLineByTime(TABLE_TIMELINE, key, sn, startTime,endTime,100);
		return timeLineList;
	}
	public List<TimeLine> getTimeLineListByTime(String key,String sn,long startTime,long endTime,int num){
		
		List<TimeLine> timeLineList = ch.getTimeLineByTime(TABLE_TIMELINE, key, sn, startTime,endTime,num);
		return timeLineList;
	}	
	public List<TimeLine> getTimeLineListByTime(String[] keyArr,String sn,long startTime,long endTime){
		List<TimeLine> timeLineList = ch.getTimeLineByTime(TABLE_TIMELINE, keyArr, sn, startTime,endTime,100);
		return timeLineList;
	}	
	public List<TimeLine> getTimeLineListByTime(String[] keyArr,String sn,long startTime,long endTime,int num){
		List<TimeLine> timeLineList = ch.getTimeLineByTime(TABLE_TIMELINE, keyArr, sn, startTime,endTime,num);
		return timeLineList;
	}
	
	
	// 用于替换掉getTimeLineList(String key,String sn,int start ,int num)
	/**
	 * 新接口某个时间点最近的分页数据
	 */
	public List<TimeLine> getTimeLineListByTime(String key,String sn,long endTime,int start ,int num){		
		List<TimeLine> timeLineList = ch.getTimeLine(TABLE_TIMELINE, key, sn,endTime,start,num);
		return timeLineList;
	}
	/**
	 * 新接口某个时间点最近的分页数据
	 */
	public List<TimeLine> getTimeLineListByTime(String[] keyArr,String sn,long endTime,int start ,int num){
		List<TimeLine> timeLineList = ch.getTimeLine(TABLE_TIMELINE, keyArr, sn, endTime,start,num);
		return timeLineList;
	}
	
	public TimeLine getTimeLineListByTime(String key,String sn ,long time){
		TimeLine tl = ch.getTimeLineByTime(TABLE_TIMELINE, key, sn, time);
		return tl;
	}
	
	/**
	 * 时间轴上此uid的SN下有总记录数N
	 */
	public int getCount(String key,String sn){
		return ch.getTimeLineTotalCount(TABLE_TIMELINE,key,sn);
	}
	
	public int getCount(String key,String sn,long startTime,long endTime){
		if(startTime >= endTime)return 0;
		return ch.getTimeLineCount(TABLE_TIMELINE,key,sn, String.valueOf(startTime),String.valueOf(endTime));
	}
	
}
