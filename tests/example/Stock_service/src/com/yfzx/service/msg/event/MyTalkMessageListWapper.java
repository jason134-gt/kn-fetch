package com.yfzx.service.msg.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.stock.common.model.share.TalkMessage;
import com.stock.common.msg.MsgConst;
import com.yfzx.service.share.MsgTimeLineService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.msg.message.IMessage;

/**
 * 消息组包装类，我的最新联系人消息【每个联系人只有一条最新消息】
 * 只给MsgConst.MSG_USER_TYPE_7类型的消息使用
 * 
 * @author：杨真
 * @date：2014-7-26
 */
public class MyTalkMessageListWapper implements Serializable {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = -6433358233048089645L;
	//private static Object _q = new Object();
	Logger log = LoggerFactory.getLogger(MyTalkMessageListWapper.class);
	List<TalkMessage> _q;
	String key;
	int maxsize = 500;

	public MyTalkMessageListWapper() {
		_q = new ArrayList<TalkMessage>();
	}

	/**
	 * 包装后的List,
	 * 删除等操作不影响内部
	 */
	public List<TalkMessage> getMessageList() {
		if (_q == null) {
			_q = new ArrayList<TalkMessage>();
		}
		return Lists.newArrayList(_q);
	}
	
	/**
	 * 删除部分消息
	 */
	public void removeAll(List<TalkMessage> tml){
		_q.removeAll(tml);
	}
	
	/**
	 * 清空 内部的List<TalkMessage>
	 */
	public void clear(){
		_q.clear();
	}

	@Override
	public String toString() {
		return "MyTalkMessageListWapper [ _q=" + _q + ", key="
				+ key + ", maxsize=" + maxsize + "]";
	}

	public MyTalkMessageListWapper(String key) {
		this.key = key;
	}

	public void put(TalkMessage item) {
		String sn = MsgTimeLineService.reSn(MsgConst.MSG_USER_TYPE_7);
		//我的uid
		String myUid = key.split("\\^")[0];
		if(item.getD() == null || item.getS() == null ||
				( myUid.equals(item.getD()) == false && myUid.equals(item.getS()) == false )){
			log.error("旧的数据 time="+ item.getTime() +" uuid=" + item.getUuid() + " uid=" + myUid + " d="+item.getD()+" s="+item.getS());
//			MsgTimeLineService.getInstance().deltetTimeLine(myUid, sn,item.getTime());
//			return;
		}
		//联系人UID 可能系统群发旧数据，2者都不一样
		String itemUidLXR = myUid.equals(item.getS())?item.getD():item.getS();
		if(itemUidLXR == null){
			log.error("删除数据有残留 time="+ item.getTime() +" uuid=" + item.getUuid() + " uid=" + myUid + " d="+item.getD()+" s="+item.getS());
			return;
		}
		// 清理过期的数据
		synchronized (_q) {
			TalkMessage finded = null;
			// 查找队列中消息是否已存在
			for (int i = 0; i < _q.size(); i++) {
				TalkMessage tm = _q.get(i);
				if (itemUidLXR.equals(tm.getD()) || itemUidLXR.equals(tm.getS()) ) {
					finded = tm;
					break;
				}
			}
			if (finded != null) {
				//Nosql处也要删除时间线 逻辑嵌入太深，且这个消息是否一定对应MsgTimeLine的talk也未知
				if(StringUtils.isNotBlank(myUid)){					
					if(item.getTime() != finded.getTime()){
						MsgTimeLineService.getInstance().deltetTimeLine(myUid, sn, finded.getTime());
					}else{
						log.info("新数据put时间重复 time="+ item.getTime() +" uuid=" + item.getUuid() + " uid=" + myUid + " d="+item.getD()+" s="+item.getS() +" 旧time="+finded.getTime());
						try{
							throw new Exception("重复加载了,此处为找出问题"
									+"\r\n新数据put时间重复 time="+ item.getTime() +" uuid=" + item.getUuid() + " uid=" + myUid + " d="+item.getD()+" s="+item.getS() +" 旧time="+finded.getTime()
									+"\r\n key="+key + " _q=" +_q);
						}catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
				_q.remove(finded);
				// 对消息数加1，后面put进去后,供前端更快获取跟这个用户的聊天消息数目
				//建议增加unreadCount
				int newCount = finded.getCount() + 1;
				item.setCount(newCount);
				if(item.getStatus()==0){
					int unreadCount = finded.getUnreadCount() + 1;//未读信息数 +1
					item.setUnreadCount(unreadCount);
				}
			} else{
				finded = item;
				if(item.getStatus()==0){
					item.setUnreadCount(1);
				}
			}
			// 如果已满，就把尾部移掉
			if (_q.size() == maxsize)
				_q.remove(_q.size() - 1);

			// 消息发过来默认是按时间降序排，但由于网络原因，顺序可能会乱，这里做个处理
			// 查找位置
			int index = _q.size();
			for (int i = 0; i < _q.size(); i++) {
				IMessage e = _q.get(i);
				if (e != null) {
					// 与前一个对比 ，如果时间序顺正常，就停止查找
					if (item.getTime() >= e.getTime()) {
						index = i;
						break;
					}

				}
			}
			if(index == maxsize){
				index = index -1;
			}
			// 加到相应位置
			_q.add(index, item);
		}
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * 从前往后，按时间降序排列
	 * 
	 * @param time
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<TalkMessage> getTalkMessageList(int ftype, long time, int limit) {
		int maxReturnSize = ConfigCenterFactory.getInt("dcss.max_blog_return_size", 35);
		List<TalkMessage> el = new ArrayList<TalkMessage>();
		List<TalkMessage> tel = null;
		if (_q.size() == 0)
			return null;
		synchronized (_q) {
			int index = _q.size();
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				TalkMessage e = _q.get(i);
				if (e != null) {
					if (e.getTime() <= time) {
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
					//el = tel.subList(start, tel.size() - 1);
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
	 * 取最新消息列表
	 * 
	 * @param etime：截止时间
	 * @param maxcount:最大条数
	 * @param 
	 * @return
	 */
	public List<TalkMessage> getTalkMessageList(long etime, int maxcount) {
		List<TalkMessage> el = new ArrayList<TalkMessage>();
		if (_q.size() == 0)
			return null;
		synchronized (_q) {
			int index = 0;
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				IMessage e = _q.get(i);
				if (e != null) {
					if (e.getTime() < etime||i>=maxcount) {
						index = i;
						break;
					}
				}
			}
			if(index!=0)
				el = Lists.newArrayList(_q.subList(0, index));
		}
		return el;
	}	
	
	public List<TalkMessage> getTalkMessageList(int start, int limit) {
		int size = _q.size();
		int end = start + limit;
		if (start > size)
			return null;
		if (end > size)
			end = size;
		return new ArrayList<TalkMessage>(_q.subList(start, end));
	}

	public int getMaxsize() {
		return maxsize;
	}

	public int getActiveSize() {
		if (_q == null) {
			return 0;
		} else {
			return _q.size();
		}
	}
}
