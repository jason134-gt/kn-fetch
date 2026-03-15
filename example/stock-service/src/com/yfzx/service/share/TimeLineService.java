package com.yfzx.service.share;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.stock.common.model.share.TimeLine;
import com.stock.common.util.CassandraHectorGateWay;
import com.stock.common.util.DateUtil;

public class TimeLineService {
	
	public static final String TABLE = "TimeLine";
	private static CassandraHectorGateWay ch = CassandraHectorGateWay.getInstance();
	
	public static enum SAVE_TABLE {
		ARTICLE("article"),//用简单family,其它用SuperFamily 后期如果有需要再修改
		COMMENT("comment"),
		VISTOR("vistor"),
		FAVORITE("favorite"),
		FOLLOW("follow"),//关注者
		BEFOLLOW("befollow"),//粉丝
		AT("at"),
		USER_EXT("userExt"),
		TOPIC("topic"),//公司用户的话题 如万科的话题
		TOPICFOLLOW("topicFollow"),//根据用户查询被关注的话题
		TOPICBEFOLLOW("topicBefollow"),//根据话题查询关注的用户
		TRADE_FOLLOW("tradeFollow"),//根据用户查询关注的“模拟炒股”
		TRADE_BEFOLLOW("tradeBefollow"),//根据“模拟炒股”查询关注的用户
		TOPICRECOMMEND("topicRecommend"),//热门话题推荐
		VIEWPOINT("viewpoint"),//观点
		NOTICE("notice")//公司公告
		;
		
		private String v;
		SAVE_TABLE(String v){
			this.v = v;
		}
		public String toString(){
			return this.v;
		}
	};
	private static TimeLineService instance = new TimeLineService();
	
	public static TimeLineService getInstance()
	{
		return instance;
	}
	
	public void saveTimeLine(String key,String uuid,SAVE_TABLE saveTable,long timemillis){
//		Map<String,Map<String,String>> timeLineMap = new HashMap<String,Map<String,String>>();
//		Map<String,String> timeLine = new HashMap<String,String>();
//		timeLine.put(String.valueOf(timemillis), uuid);
//		timeLineMap.put(saveTable.toString(), timeLine);
//		ch.insertSuper(TABLE,key,timeLineMap);	
		TimeLine tl = new TimeLine(String.valueOf(timemillis), uuid);
		ch.insertTimeLine(TABLE,key,saveTable.toString(),tl);
	}
	/**
	 * 删除时光轴上的内容
	 * ColunFamily--[key]--SuperColumn[sn]<br>
	 * 						    |--Column_1[name]<br>
	 * 							|		|--[value]<br>
	 * 							|--Column_...<br>
	 * @param key = [key]
	 * @param saveTable = [sn]
	 * @param timemillis = [name]
	 */
	public void deltetTimeLine(String key,SAVE_TABLE saveTable,long timemillis){
		ch.deltetNameInSuperColumn(TABLE, key,saveTable.toString(), String.valueOf(timemillis));
	}
	//删除消息中心对应的nosql
	public void deltetTimeLine(String key,String msgType,long timemillis){
		ch.deltetNameInSuperColumn(TABLE, key,msgType, String.valueOf(timemillis));
	}
	//如删除时光轴上的文章 [文章里有评论列表]
	public void deltetTimeLine(String key,SAVE_TABLE saveTable){
		ch.deleteSuperColumn(TABLE, key,saveTable.toString());
	}
	
	/**
	 * @param uid
	 * @param saveTable
	 * @return 
	 */
	public List<String> getTimeLine(String key,SAVE_TABLE saveTable){
		return getTimeLine(key,saveTable,0,0);
	}
	
	/**
	 * @deprecated
	 * @param uid
	 * @param saveTable
	 * @param start
	 * @param num
	 * @return
	 */
	public List<String> getTimeLine(String key,SAVE_TABLE saveTable,int start ,int num){
		List<TimeLine> timeLineList = ch.getTimeLine(TABLE,key,saveTable.toString(),start,num);		
		List<String> uuidList = new ArrayList<String>();
		for(TimeLine tl : timeLineList) {			
			uuidList.add(tl.getUuid());
		}
		return uuidList;
	}
	
	
	/**
	 * @deprecated
	 */
	public List<TimeLine> getTimeLineList(String key,SAVE_TABLE saveTable,int start ,int num){
		List<TimeLine> timeLineList = ch.getTimeLine(TABLE,key,saveTable.toString(),start,num);
		return timeLineList;		
	}
	/**
	 * @deprecated
	 */
	public List<TimeLine> getTimeLineList(String[] keyArr,SAVE_TABLE saveTable,int start ,int num){
		List<TimeLine> timeLineList = ch.getTimeLine(TABLE,keyArr,saveTable.toString(),start,num);
		return timeLineList;		
	}
	
	
	/**
	 * 此类接口 用于按时间范围分页 比如查询24小时的文章
	 */
	public List<TimeLine> getTimeLineListByTime(String key,SAVE_TABLE saveTable,long startTime,long endTime){		
		List<TimeLine> timeLineList = ch.getTimeLineByTime(TABLE, key, saveTable.toString(), startTime,endTime,100);
		return timeLineList;
	}
	public List<TimeLine> getTimeLineListByTime(String key,SAVE_TABLE saveTable,long startTime,long endTime,int num){
		
		List<TimeLine> timeLineList = ch.getTimeLineByTime(TABLE, key, saveTable.toString(), startTime,endTime,num);
		return timeLineList;
	}	
	public List<TimeLine> getTimeLineListByTime(String[] keyArr,SAVE_TABLE saveTable,long startTime,long endTime){
		List<TimeLine> timeLineList = ch.getTimeLineByTime(TABLE, keyArr, saveTable.toString(), startTime,endTime,100);
		return timeLineList;
	}	
	public List<TimeLine> getTimeLineListByTime(String[] keyArr,SAVE_TABLE saveTable,long startTime,long endTime,int num){
		List<TimeLine> timeLineList = ch.getTimeLineByTime(TABLE, keyArr, saveTable.toString(), startTime,endTime,num);
		return timeLineList;
	}
	
	
	// 用于替换掉getTimeLineList(String key,SAVE_TABLE saveTable,int start ,int num)
	/**
	 * 新接口某个时间点最近的分页数据
	 */
	public List<TimeLine> getTimeLineListByTime(String key,SAVE_TABLE saveTable,long endTime,int start ,int num){		
		List<TimeLine> timeLineList = ch.getTimeLine(TABLE, key, saveTable.toString(),endTime,start,num);
		return timeLineList;
	}
	/**
	 * 新接口某个时间点最近的分页数据
	 */
	public List<TimeLine> getTimeLineListByTime(String[] keyArr,SAVE_TABLE saveTable,long endTime,int start ,int num){
		List<TimeLine> timeLineList = ch.getTimeLine(TABLE, keyArr, saveTable.toString(), endTime,start,num);
		return timeLineList;
	}
	
	
	
	public List<String> getTimeLineByLatest(String key,SAVE_TABLE saveTable){
		return getTimeLine(key,saveTable,0,20);
	}
	
	/**
	 * 时间轴上此uid的SN下有总记录数N
	 */
	public int getCount(String key,SAVE_TABLE saveTable){
		return ch.getTimeLineTotalCount(TABLE,key,saveTable.toString());
	}
	
	public int getCount(String key,SAVE_TABLE saveTable,long startTime,long endTime){
		if(startTime >= endTime)return 0;
		return ch.getTimeLineCount(TABLE,key,saveTable.toString(), String.valueOf(startTime),String.valueOf(endTime));
	}
	
	public static void main(String[] args) {
		String sKey = "10013";
		String sname = "article";
		long endTime = System.currentTimeMillis();
		long startTime = endTime - 1000l*3600l*24l*30l;//注意要使用Long型 recent days
		List<TimeLine> tlList = ch.getTimeLineByTime(TABLE, sKey, sname, startTime,endTime,20);
		for(TimeLine tl : tlList){
			long timemillis = Long.parseLong(tl.getTimeMillis());
			System.out.println(tl.getUuid() + " === " + DateUtil.getSysDate(DateUtil.YYYYMMDDHHMMSS,new Date(timemillis)));
		}
		System.out.println("数量=" + tlList.size());
	}

}
