package com.yfzx.service.msg.event;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.elasticsearch.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.msg.MsgConst;
import com.stock.common.msg.UserMsg;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.mycore.msg.message.IMessage;

/**
 * 消息组包装类，带过期时间
 *
 * @author：杨真
 * @date：2014-7-26
 */
public class IMessageListWapper implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 7904955434630050336L;
	private int unReadCount;//未读消息数
	/**
	 *
	 */
	Logger log = LoggerFactory.getLogger(IMessageListWapper.class);
	List<IMessage> _q;
	String key;
	int maxsize = 500;

	public IMessageListWapper() {
		_q = new ArrayList<IMessage>();
	}

	/**
	 * @param qsize 设置消息List大小
	 */
	public IMessageListWapper(int qsize) {
		maxsize = qsize;
		_q = new ArrayList<IMessage>(qsize);
	}


	/**
	 * 包装后的List,可以使用size等方法<br>
	 * 删除等操作不能影响内部<br>
	 * 外部的eq.getMessageList().clear或remove方法都有改造成eq.clear
	 */
	public List<IMessage> getMessageList() {
		if (_q == null) {
			_q = new ArrayList<IMessage>();
		}
		return Lists.newArrayList(_q);
	}

	/**
	 * 删除部分消息
	 */
	public void removeAll(List<? extends IMessage> iml){
		_q.removeAll(iml);
	}
	/**
	 * 删除部分消息
	 */
	public void remove(IMessage im){
		_q.remove(im);
	}

	/**
	 * 清空 内部的List<? extends IMessage>
	 */
	public void clear(){
		_q.clear();
	}

	public IMessageListWapper(String key) {
		this.key = key;
	}

	public void put(IMessage item) {
		if(StringUtils.isBlank(String.valueOf(((UserMsg)item).getAttr("uuid")))){
			try{
				throw new Exception("Uuid非法,此处为找出问题"+_q);
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		synchronized (_q) {
			UserMsg um = (UserMsg)item;
			int index =  _q.size();
			 //关注用户消息去重 防止多客户端执行关注操作出现重复记录
			if(um.getMsgType() == MsgConst.MSG_USER_TYPE_5){
				for (int i = 0; i < _q.size(); i++) {
					UserMsg e = (UserMsg)_q.get(i);
					if(um.getS().equals(e.getS())){
						log.info(um.getD()+" 关注对象已经存在缓存列表 "+ um.getS());
						return;
					}
				}
			}
			/*if(um.getMsgType() == MsgConst.MSG_USER_TYPE_0){
				List<IMessage> removeList = Lists.newArrayList();
				for (int i = 0; i <  _q.size(); i++) {
					UserMsg e = (UserMsg)_q.get(i);
					String uuid = um.getAttr("uuid");
					if(uuid==null){
						return;
					}
					if(e.getAttr("uuid").equals(uuid)){
						log.info("哎呦喂 有重复的博文 删除之..."+e.getAttr("uuid"));
						removeList.add(e);
						break;
					}
				}
				if(removeList.size()>0){
					_q.removeAll(removeList);
				}
			}*/
			// 如果已满，就把尾部移掉
			if (index == maxsize)
				_q.remove(index - 1);
			// 消息发过来默认是按时间降序排，但由于网络原因，顺序可能会乱，这里做个处理
			// 查找位置

			for (int i = 0; i < _q.size(); i++) {
				IMessage e = _q.get(i);
				if (e != null) {

					// 与前一个对比 ，如果时间顺序正常，就停止查找
					if (item.getTime() >= e.getTime()) {
						index = i;
						break;
					}
				}
			}
			if(um.getS()!=null && um.getD()!=null){
					if(index == maxsize){
						index = index -1;
					}
					if(index>_q.size()){
						index = _q.size();
					}
					_q.add(index, item);
					this.setUnReadCount(this.getUnReadCount()+1);

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
	 * 从前往后，按时间降序排列
	 *
	 * @param time
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<IMessage> getIMessageList(int ftype, long time, int limit) {
		List<IMessage> el = new ArrayList<IMessage>();
		List<IMessage> tel = null;
		if (_q.size() == 0)
			return null;
		synchronized (_q) {
			int index = _q.size();
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				IMessage e = _q.get(i);
				if (e != null) {
					if (e.getTime() <= time) {
						if (ftype == 0) {
								index = i;
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
					//el = tel.subList(start, tel.size() - 1);
					el = Lists.newArrayList(tel.subList(start, tel.size()));
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
	 * 从前往后，按时间降序排列(重写)
	 *
	 * @param time
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<IMessage> getIMessageList(int ftype, long time, int s,int limit) {
		List<IMessage> el = new ArrayList<IMessage>();
		List<IMessage> tel = null;
		if (_q.size() == 0)
			return null;
		synchronized (_q) {
			int index = 0;
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				IMessage e = _q.get(i);
				if (e != null) {
					if (e.getTime() <= time) {
						if (ftype == 0) {
								index = i;
						} else {
							if(index>=_q.size())
							{
								index=_q.size()-1;
							}
							else
								index = i + 1;
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
					if (start <= 0)
						start = 0;
					el = Lists.newArrayList(tel.subList(start, tel.size()));
				}

			}

			// 往后
			if (ftype == 1) {
				tel = _q.subList(index, _q.size());
				if (tel.size() > 0) {
					int end = limit + s;
					if (end > tel.size())
						end = tel.size();
					if (s > end)
						s = end;
					el = Lists.newArrayList(tel.subList(s, end));
				}

			}

		}
		return el;
	}

	/**
	 * 从前往后，按时间降序排列(根据时间 取time之前的记录)
	 * @param time
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<IMessage> getMessageList(long time, int start, int limit)
	{
		List<IMessage> el = new ArrayList<IMessage>();
		if(_q.size()==0)
			return el;
		for(int i=0;i<_q.size();i++)
		{
			IMessage e = _q.get(i);
			if(e!=null&&e.getTime()<=time&&i>=start&&i<(start+limit))
				el.add(e);
			else
			{
				//由于是按时间排好序的，所以出以下情况，就可以中止循环，减少不必要的循环
				if(i>=(start+limit))
					break;
			}
		}
		return el;
	}

	/**
	 * 从前往后，按时间降序排列
	 *
	 * @param time
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<IMessage> getMessageList(int ftype, long time, int limit) {
		int maxReturnSize = ConfigCenterFactory.getInt("dcss.max_blog_return_size", 35);
		List<IMessage> el = new ArrayList<IMessage>();
		List<IMessage> tel = null;
		if (_q.size() == 0)
			return null;
		synchronized (this) {
			int index = _q.size();
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				IMessage e = _q.get(i);
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
	 * 按时间获取最新观点数量
	 * @param time
	 * @param start
	 * @return
	 */
	public int getMessageListCount(int ftype, long time) {
		List<IMessage> tel = null;
		int count = 0;
		if (_q.size() == 0)
			return count;
		synchronized (this) {
			int index = _q.size();
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				IMessage e = _q.get(i);
				if (e != null) {
					if (e.getTime() <= time) {
						if (ftype == 0) {
								index = i;
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
				tel = _q.subList(0, index );
				if (tel.size() > 0) {
//					int start = index;
//					if (start < 0)
//						start = 0;
//					int end = tel.size();
//					count = end - start > 0 ? end - start : 0;
					count = tel.size();
				}
			}

			// 往后
			if (ftype == 1) {
				if(time==0){
					index = 0;
				}
				tel = _q.subList(index, _q.size());
				if (tel.size() > 0) {
					count = tel.size();
				}
			}
		}
		return count;
	}

	/**
	 * 从前往后，按时间降序排列(根据时间 取time之后的记录)
	 * @param time
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<IMessage> getMessageListAfterTime(long time, int start, int limit)
	{
		List<IMessage> el = new ArrayList<IMessage>();
		if(_q.size()==0)
			return el;
		for(int i=0;i<_q.size();i++)
		{
			IMessage e = _q.get(i);
			if(e!=null&&e.getTime()>time){
				el.add(e);
			}
			else
			{
				//由于是按时间排好序的，所以出以下情况，就可以中止循环，减少不必要的循环
				//if(i>=(start+limit))
					break;
			}
		}
		return el;
	}

	@Override
	public String toString() {
		return "IMessageListWapper [_q=" + _q + ", key=" + key + ", maxsize="
				+ maxsize + "]";
	}

	public List<IMessage> getTalkMessageList(int start, int limit) {
		int size = _q.size();
		int end = start + limit;
		if (start > size)
			return null;
		if (end > size) {
			end = size;
		}
		// ArrayList.subList() 这个方法返回的无论是 SubList 还是 RandomAccessSubList 都没有实现
		// Serializable 接口，不能被序列化
		return new ArrayList<IMessage>(_q.subList(start, end));
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

	public int getUnReadCount() {
		return unReadCount<0?0:unReadCount;
	}

	public void setUnReadCount(int unReadCount) {
		this.unReadCount = unReadCount;
	}

//	public static void main(String[] args) {
//		IMessageListWapper imlw = new IMessageListWapper(500);
//		List<UserMsg> umList_1 = new ArrayList<UserMsg>();
//		List<UserMsg> umList_2 = new ArrayList<UserMsg>();
//		for(int i=0;i<1000;i++){
//			UserMsg um = new UserMsg();
//			um.setD(""+i);
//			um.setS(""+i);
//			um.setUuid(""+i);
//			um.setTime(Long.valueOf(i));
//			if(i%2 ==1){
//				umList_1.add(um);
//			}else{
//				umList_2.add(um);
//			}
//		}
//
//
//		Runnable r1 = new Runnable() {
//			List<UserMsg> umList;
//			IMessageListWapper imlw;
//			public Runnable set(List<UserMsg> umList,IMessageListWapper imlw){
//				this.umList = umList;
//				this.imlw = imlw;
//				return this;
//			}
//
//			@Override
//			public void run() {
//				for(UserMsg um : umList){
//					imlw.put(um);
//				}
//			}
//		}.set(umList_1, imlw);
//		Runnable r2 = new Runnable() {
//			List<UserMsg> umList;
//			IMessageListWapper imlw;
//			public Runnable set(List<UserMsg> umList,IMessageListWapper imlw){
//				this.umList = umList;
//				this.imlw = imlw;
//				return this;
//			}
//
//			@Override
//			public void run() {
//				for(UserMsg um : umList){
//					imlw.put(um);
//				}
//			}
//		}.set(umList_2, imlw);
//		r1.run();
//		r2.run();
//
//		try {
//			Thread.sleep(1000l);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		{
//			System.out.println("########################");
//			List<IMessage> imList = imlw.getMessageList();
//
//			for(IMessage im : imList){
//				UserMsg um = (UserMsg)im;
//				System.out.println(um.getD()+"=="+im.getTime());
//			}
//		}
//		{
//			System.out.println("########################");
//			List<IMessage> imList = imlw.getMessageList(1, 520l, 10);
//			for(IMessage im : imList){
//				UserMsg um = (UserMsg)im;
//				System.out.println(um.getD()+"=="+im.getTime());
//			}
//		}
//		{
//			System.out.println("########################");
//			List<IMessage> imList = imlw.getMessageList(0, 520l, 10);
//			for(IMessage im : imList){
//				UserMsg um = (UserMsg)im;
//				System.out.println(um.getD()+"=="+im.getTime());
//			}
//
//			imlw.removeAll(imList);
//			System.out.println("########################");
//			System.out.println(imlw.getMessageList().size());
//			UserMsg um = new UserMsg();
//			int i = 10000;
//			um.setD(""+i);
//			um.setS(""+i);
//			um.setUuid(""+i);
//			um.setTime(Long.valueOf(i));
//			imlw.getMessageList().add(um);
//			System.out.println("########################");
//			System.out.println(imlw.getMessageList().size());
//			imlw.put(um);
//			imList = imlw.getMessageList();
//			System.out.println("########################");
//			System.out.println(imList.size());
//		}
//	}
}
