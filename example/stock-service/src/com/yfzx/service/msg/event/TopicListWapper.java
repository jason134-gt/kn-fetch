package com.yfzx.service.msg.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import topic.TopicConst;

import com.stock.common.constants.TopicConstants;
import com.stock.common.model.Topic;
import com.stock.common.model.TopicUser;
import com.yfzx.service.client.RemindServiceClient;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.lcs.cache.CacheUtil;
import com.yz.mycore.lcs.enter.LCEnter;

/**
 * 消息组包装类，带过期时间
 * 
 * @author：杨真
 * @date：2014-7-26
 */
public class TopicListWapper implements Serializable {
	private static final long serialVersionUID = 7904955434630050336L;
	Logger log = LoggerFactory.getLogger(TopicListWapper.class);
	static int maxsize = 500;
	List<Topic> q_discuss;
	List<Topic> q_time;
	List<Topic> q_read;
	List<Topic> q_all;
	//机会话题
	List<Topic> q_stock_chance;
	//普通话题
	List<Topic> q_nomal;
	String key;
	private int type;
	
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public TopicListWapper() {
		q_time = new ArrayList<Topic>();
		q_discuss = new ArrayList<Topic>();
		q_read = new ArrayList<Topic>();
		q_all = new ArrayList<Topic>();
		q_nomal = new ArrayList<Topic>();
		q_stock_chance = new ArrayList<Topic>();
		
	}
	
	/**
	 * @param qsize 设置消息List大小
	 */
	public TopicListWapper(int qsize) {
		maxsize = qsize;
		q_time = new ArrayList<Topic>(qsize);
	}	
	
	
	/**
	 * 包装后的List,可以使用size等方法<br>
	 * 删除等操作不能影响内部<br>
	 * 外部的eq.getMessageList().clear或remove方法都有改造成eq.clear
	 */
	public List<Topic> getTopicVOListByType(int type) {
		List<Topic> list; 
		switch (type) {
		case TopicConstants.TOPIC_TIME_LIST:
			list = q_time;
			break;
		case TopicConstants.TOPIC_DISCUSS_LIST:
			list = q_discuss;
			break;
		case TopicConstants.TOPIC_READ_LIST:
			list = q_read;
			break;
		case TopicConstants.TOPIC_ALL_LIST:
			list = q_all;
			break;
		default:
			list = q_time;
			break;
		}
		return list;
	}
	
	//区分机会话题和普通话题
	public List<Topic> getByType(int type){
		List<Topic> result = Lists.newArrayList();
		List<Topic> list = getTopicVOListByType(TopicConstants.TOPIC_TIME_LIST);
		if(q_nomal == null){
			q_nomal = new ArrayList<Topic>();
		}
		if(q_stock_chance == null){
			q_stock_chance = new ArrayList<Topic>();
		}
		if(q_nomal.size()==0 || q_stock_chance.size()==0){
			for(Topic topic : list){
				if(topic.getType() == TopicConst.STOCK_CHANCE_TOPIC){
					q_stock_chance.add(topic);
				}else{
					q_nomal.add(topic);
				}
			}
		}
		switch (type) {
			case TopicConst.STOCK_CHANCE_TOPIC:
				result = q_stock_chance;
				break;
			case TopicConst.COMMONE_TOPIC:
				result = q_nomal;
				break;
			case TopicConst.All_TOPIC:
				result = list;
				break;
			default:
				result = list;
				break;
		}
		return result;
	}
	
	/**
	 * 删除部分消息
	 */
	public void removeAll(List<? extends Topic> iml){
		q_time.removeAll(iml);
	}
	
	public void clearAll(){
		q_time.clear();
		q_read.clear();
		q_discuss.clear();
	}
	
	//话题是否通过审核
	private boolean passed(String identify){
		String c = CacheUtil.getCacheName(TopicConstants.TOPICUSER);
		TopicUser tu = LCEnter.getInstance().get(identify, c);
		if(tu==null || tu.getStatus2()==TopicConstants.TOPIC_UNPASSED 
				|| tu.getStatus2()==TopicConstants.TOPIC_PASSING){
			return false;
		}
		return true;
	}
	
	public void remove(Topic item){
		q_time.remove(item);
		q_read.remove(item);
		q_discuss.remove(item);
		int q_discuss_index = (Integer)item.getAttr("d_index")==null?-1:(Integer)item.getAttr("d_index");
		int q_read_index = (Integer)item.getAttr("r_index")==null?-1:(Integer)item.getAttr("r_index");
		if(q_discuss_index>=0){
			List<Topic> sub_q_read = q_read.subList(q_read_index, q_read.size());
			for(Topic topic : sub_q_read){
				int index = (Integer)topic.getAttr("r_index");
				index = index-1<0?0:index-1;
				topic.putAttr("r_index",index);
			}
		}
		if(q_read_index>=0){
			List<Topic> sub_q_discuss = q_read.subList(q_discuss_index, q_discuss.size());
			for(Topic topic : sub_q_discuss){
				int index = (Integer)topic.getAttr("d_index");
				index = index-1<0?0:index-1;
				topic.putAttr("d_index",index);
			}
		}
	}
	
	public TopicListWapper(String key) {
		this.key = key;
	}

	//按讨论数和时间排序
	public void sort(Topic item){
		if(item==null || !passed(item.getUidentify())){
			return;
		}
		//给q_discuss 排序
		try {
			synchronized (this) {
				Integer index = (Integer)item.getAttr("d_index");//拿到在q_discuss中的下标
				if(index != null){//update
					index = index.intValue();
					if( index < 0 && q_discuss.size() < index){
						log.info("错误的排序规则");
						return;
					}
					int perIndex = index-1<0?0:index-1;
					Topic perItem = q_discuss.get(perIndex);
					if(item.getDiscussNum()>perItem.getDiscussNum()){
						int ppindex = perIndex-1;;
						for(int i=0;i<15;i++){
							if(ppindex<0){
								perIndex = 0;
								break;
							}
							Topic ppitem = q_discuss.get(ppindex);
							if(ppitem.getDiscussNum()>perItem.getDiscussNum()){
								perIndex = (Integer)ppitem.getAttr("d_index")+1;
								break;
							}
							ppindex--;
							int d = (Integer)ppitem.getAttr("d_index")+1;
							ppitem.putAttr("d_index", d);
						}
						item.putAttr("d_index", perIndex);
						perItem.putAttr("d_index",index);
						q_discuss.remove(item);
						q_discuss.add(perIndex,item);
					}
				}else{//add
					addNewItem2List(item);
				}
				//给q_time 排序
				if(q_time.contains(item)){
					q_time.remove(item);
				}
				q_time.add(0, item);
				put2All(item);
			}
		} catch (Exception e) {
			log.info("TopicListWapper sort fail " +e);
		}
	}
	
	//新增的话题加入排序队列
	public void addNewItem2List(Topic item){
		int last =q_discuss.size()-1 < 0 ? 0 : q_discuss.size()-1;
		item.putAttr("d_index", last);
		q_discuss.add(last,item);
		int last2 =q_read.size()-1 < 0 ? 0 : q_read.size()-1;
		item.putAttr("r_index", last2);
		q_read.add(last2,item);
	}
	
	//按阅读数排序
	public void sortForRead(Topic item){
		try {
			Integer index = (Integer)item.getAttr("r_index");//拿到在q_read中的下标
			if(index == null){
				//不做处理
			}else{
				index = index.intValue();
				if(index<0 || q_read.size()<=index || !passed(item.getUidentify())){
					log.info("错误的排序规则  sortForRead");
					return;
				}
				synchronized (this){
					int perIndex = index-1<0?0:index-1;
					Topic perItem = q_read.get(perIndex);
					if(item.getReadNum()>perItem.getReadNum()){
						int ppindex = perIndex-1;;
						for(int i=0;i<15;i++){
							if(ppindex<0){
								perIndex = 0;
								break;
							}
							Topic ppitem = q_read.get(ppindex);
							if(ppitem.getReadNum()>perItem.getReadNum()){
								perIndex = (Integer)ppitem.getAttr("r_index")+1;
								break;
							}
							ppindex--;
							int d = (Integer)ppitem.getAttr("r_index")+1;
							ppitem.putAttr("r_index", d);
						}
						item.putAttr("r_index", perIndex);
						perItem.putAttr("r_index", index);
						q_read.remove(item);
						q_read.add(perIndex,item);
					}
				}
			}
		} catch (Exception e) {
			log.info("error"+e);
		}
	}
	
	public void put(Topic item) {
		if(item==null){
			log.info("TopicvoListWapper item is null");
			return;
		}
		if(q_time.size()>maxsize){
			log.info("list size bigger then maxsize!");
			return;
		}
		if(q_time.contains(item)){
			q_time.remove(item);
		}
		if(q_discuss.contains(item)){
			q_discuss.remove(item);
		}
		if(q_read.contains(item)){
			q_read.remove(item);
		}
		q_time.add(item);
		q_discuss.add(item);
		q_read.add(item);
		
		if(item.getType() == TopicConst.STOCK_CHANCE_TOPIC){
			if(!q_stock_chance.contains(item)){
				q_stock_chance.add(0,item);
			}else if(!q_nomal.contains(item)){
				q_nomal.add(0,item);
			}
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * 按维度 分页（时间，阅读数，浏览量）
	 * @param pageIndex
	 * @param limit
	 * @param type
	 * @return
	 */
	public List<Topic> getListForPage(int pageIndex,int limit,int type) {
		List<Topic> el = new ArrayList<Topic>();
		List<Topic> list = getTopicVOListByType(type);
		int start = (pageIndex-1)*limit;
		int count = list.size();
		if(list!=null){
			if(start>list.size()){
				start = count;
			}
		}
		int end = limit+start;
		if(end>count){
			end = count;
		}
		el = Lists.newArrayList(list.subList(start,end));
		return el;
	}
	/**
	 * 按话题类型 分页（资讯，策略）
	 * @param pageIndex
	 * @param limit
	 * @param type
	 * @return
	 */
	public Map<String,Object> getListForPage2(int pageIndex,int limit,int type) {
		Map<String,Object> rmap = Maps.newHashMap();
		List<Topic> el = new ArrayList<Topic>();
		List<Topic> list = getByType(type);
		int start = (pageIndex-1)*limit;
		int count = list.size();
		if(list!=null){
			if(start>list.size()){
				start = count;
			}
		}
		int end = limit+start;
		if(end>count){
			end = count;
		}
		el = Lists.newArrayList(list.subList(start,end));
		rmap.put("list", el);
		rmap.put("count", count);
		return rmap;
	}
	
	
	/**
	 * 从前往后，按时间降序排列
	 *
	 * @param ftype 0 最新 1历史
	 * @param time
	 * @param limit
	 * @return
	 */
	public List<Topic> getLastistList(int ftype, long time, int limit) {
		List<Topic> _q = q_time;
		if(ftype==1){
			time = time-1;
		}else{
			time = time+1;
		}
		int maxReturnSize = ConfigCenterFactory.getInt("dcss.max_blog_return_size", 35);
		List<Topic> el = new ArrayList<Topic>();
		List<Topic> tel = null;
		if (_q.size() == 0)
			return null;
		synchronized (this) {
			int index = _q.size();
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				Topic e = _q.get(i);
				if (e != null) {
					if (e.getLastUpTime() <= time) {
						if (ftype == 0) {
							index = i;
							if(index > (maxReturnSize+limit)){
								break;
							}
								
						} else {
							if(index>_q.size())
							{
								index=_q.size()-1;
							}
							else
								index = i;
						}

						break;
					}
				}
			}
			// 往前
			if (ftype == 0) {
				tel = _q.subList(0, index);
				if (tel.size() > 0) {
					int start = index - limit;
					if (start < 0)
						start = 0;
					int end = tel.size();
					//这里需要处理返回记录条数过大，导致客户端反应过慢的问题
					if(index > (maxReturnSize+limit)){
						start = 0;
						end = maxReturnSize;
					}
					el = Lists.newArrayList(tel.subList(start, end));
				}

			}
			// 往后
			if (ftype == 1) {
				if(time==0){
					index = 0;
				}
				tel = _q.subList(index, _q.size());
				if (tel.size() > 0) {
					int end = limit;
					if (end > tel.size())
						end = tel.size();
					//el = tel.subList(0, end);
					el = Lists.newArrayList(tel.subList(0, end));
				}

			}
		}
		return el;
	}
	/**
	 * 从前往后，按讨论数降序排列
	 *
	 * @param ftype 0 最新 1历史
	 * @param num
	 * @param limit
	 * @return
	 */
	public List<Topic> getDiscussList(int ftype, long num, int limit) {
		List<Topic> _q = q_discuss;
		int maxReturnSize = ConfigCenterFactory.getInt("dcss.max_blog_return_size", 35);
		List<Topic> el = new ArrayList<Topic>();
		List<Topic> tel = null;
		if (_q.size() == 0)
			return null;
		synchronized (this) {
			int index = _q.size();
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				Topic e = _q.get(i);
				if (e != null) {
					if (e.getDiscussNum() <= num) {
						if (ftype == 0) {
							index = i;
							if(index > (maxReturnSize+limit)){
								break;
							}
						} else {
							if(index>_q.size())
							{
								index=_q.size()-1;
							}
							else
								index = i;
						}
						
						break;
					}
				}
			}
			// 往前
			if (ftype == 0) {
				tel = _q.subList(0, index);
				if (tel.size() > 0) {
					int start = index - limit;
					if (start < 0)
						start = 0;
					int end = tel.size();
					//这里需要处理返回记录条数过大，导致客户端反应过慢的问题
					if(index > (maxReturnSize+limit)){
						start = 0;
						end = maxReturnSize;
					}
					el = Lists.newArrayList(tel.subList(start, end));
				}
				
			}
			// 往后
			if (ftype == 1) {
				if(num==0){
					index = 0;
				}
				tel = _q.subList(index, _q.size());
				if (tel.size() > 0) {
					int end = limit;
					if (end > tel.size())
						end = tel.size();
					//el = tel.subList(0, end);
					el = Lists.newArrayList(tel.subList(0, end));
				}
				
			}
		}
		return el;
	}

	//添加在原位置
	public void add2(Topic item){
		q_read.remove(item);
		q_discuss.remove(item);
		q_time.remove(item);
		int r_index = (Integer)item.getAttr("r_index");
		int d_index = (Integer)item.getAttr("d_index");
		q_read.add(r_index,item);
		q_discuss.add(d_index,item);
		for(int i = 0;i<q_time.size();i++){
			//TODO 可以优化成２分查找
			if(q_time.get(i).getLastUpTime()<=item.getLastUpTime()){
				q_time.add(i,item);
				break;
			}
		}
	}
	
	public void add(Topic item) {
		int read_index = 0;
		int discuss_index = 0;
		int time_index = 0;
		synchronized (item) {
			if(!q_read.contains(item)){
				for(;read_index<q_read.size();read_index++){
					//TODO 可以优化成２分查找
					int index = -1;
					if(q_read.get(read_index).getReadNum()<=item.getReadNum()){
						index = read_index;
						item.putAttr("r_index", index);
						q_read.add(index,item);
					}
					if(index>0){
						q_read.get(read_index).putAttr("r_index", read_index+1);
					}
				}
			}
			if(!q_discuss.contains(item)){
				for(;discuss_index<q_discuss.size();discuss_index++){
					//TODO 可以优化成２分查找
					int index = -1;
					if(q_discuss.get(discuss_index).getDiscussNum()<=item.getDiscussNum()){
						index = discuss_index;
						item.putAttr("d_index", index);
						q_discuss.add(index,item);
					}
					if(index>0){
						q_discuss.get(discuss_index).putAttr("d_index", discuss_index+1);
					}
					
				}
				
			}
			
			if(!q_time.contains(item)){
				for(;time_index<q_time.size();time_index++){
					//TODO 可以优化成２分查找
					if(q_time.get(time_index).getLastUpTime()<=item.getLastUpTime()){
						q_time.add(time_index,item);
					}
				}
			}
			
			if(item.getType()== TopicConst.STOCK_CHANCE_TOPIC){
				if(!q_stock_chance.contains(item)){
					q_stock_chance.add(0,item);
				}else if(!q_nomal.contains(item)){
					q_nomal.add(0,item);
				}
			}
		}
		
	}

	public void put2All(Topic t) {
		if(q_all.contains(t)){
			q_all.remove(t);
		}
		q_all.add(t);
		
	}

	//未关注话题列表分页
	public Map<String,Object> getUnfocusListByUid(int start, int limit, int type,long uid) {
		String strUid = String.valueOf(uid);
		List<Topic> result = Lists.newArrayList();
		List<Topic> el = new ArrayList<Topic>();
		List<Topic> list = getByType(type);
		Map<String,Object> rmap = Maps.newHashMap();
		List<String> focusList = RemindServiceClient.getInstance().getUserTopicRelationship(strUid);//我关注的话题
		if(focusList!=null){
			for(Topic t : list){
				if(t==null){
					continue;
				}
				if(!focusList.contains(t.getUidentify())){
					result.add(t);
				}
			}
		}
		int count = result.size();
		if(start>result.size()){
			start = count;
		}
		int end = limit+start;
		if(end>count){
			end = count;
		}
		el = Lists.newArrayList(result.subList(start,end));
		rmap.put("list", el);
		rmap.put("total", count);
		return rmap;
	}
	
}
