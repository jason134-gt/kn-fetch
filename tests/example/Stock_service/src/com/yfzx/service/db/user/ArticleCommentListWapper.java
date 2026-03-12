package com.yfzx.service.db.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.stock.common.model.share.Comment;
import com.yz.mycore.msg.message.IMessage;
/**
 * 消息组包装类，带过期时间
 * 由Comment[收藏]转成 Comment @author tangbinqi 20140819
 * @author：杨真 
 * @date：2014-7-26
 */
public class ArticleCommentListWapper implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7824414181229037973L;
	Logger log = LoggerFactory.getLogger(ArticleCommentListWapper.class);
	private static Object lockObj = new Object();
	List<Comment> _q;
	String key;
	int qsize = 500;

	public ArticleCommentListWapper() {
		_q = new ArrayList<Comment>(qsize);
	}

	public ArticleCommentListWapper(int qsize) {
		_q = new ArrayList<Comment>(qsize);
	}
	public List<Comment> getQueue() {
		if (_q == null) {
			_q = new ArrayList<Comment>(qsize);
		}
		return Lists.newArrayList(_q);

	}
	
	/**
	 * 包装后的List,可以使用size等方法<br>
	 * 删除等操作不能影响内部<br>
	 * 外部的eq.getCommentList().clear或remove方法都有改造成eq.clear
	 */
	public List<Comment> getCommentList() {
		if (_q == null) {
			_q = new ArrayList<Comment>();
		}
		return Lists.newArrayList(_q);
	}
	
	public void removeAll(List<Comment> c){
		_q.removeAll(c);
	}
	
	public void clear(){
		_q.clear();
	}

	public ArticleCommentListWapper(String key) {
		this.key = key;
	}

	public void put(Comment item) {
		synchronized(this)
		{
			//如果已满，就把尾部移掉
			if(_q.size()==qsize)
				_q.remove(_q.size()-1);
			//加到头部
			_q.add(0, item);
		}
	}
	
	/**
	 * 本方法 主要用于初始加载 tbq
	 * @param itemList 倒序数组 0元素时间最近
	 */
	public void putAll(List<Comment> itemList){
		synchronized(this)
		{
			//加到头部
			_q.addAll(0,itemList);
			//去除超出的元素 
			if(_q.size() > qsize){
				//倒序删除，不会触发 List对象的System.arraycopy过程
				for(int i=_q.size()-1;i>=0;i--){ 
				    if(i >= qsize){
				    	_q.remove(i);
				    }
				}
//				_q.removeAll(_q.subList(qsize, _q.size()));
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
	 * @param time
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<Comment> getCommentList(int start, int limit)
	{
		List<Comment> el = new ArrayList<Comment>();
		if(_q.size()==0)
			return null;
		for(int i=0;i<_q.size();i++)
		{
			Comment e = _q.get(i);
			if(e!=null && i>=start && i<(start+limit))
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
	 * 从前往后，按时间降序排列(根据时间 取time之前的博文记录)
	 * @param time
	 * @param start
	 * @param limit
	 * @return
	 */
	public List<Comment> getCommentListBeforTime(long time, int start, int limit)
	{
		List<Comment> el = new ArrayList<Comment>();
		if(_q.size()==0)
			return null;
		for(int i=0;i<_q.size();i++)
		{
			Comment e = _q.get(i);
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
	public List<Comment> getIMessageList(int ftype, long time, int limit) {
		List<Comment> el = new ArrayList<Comment>();
		List<Comment> tel = null;
		if (_q.size() == 0)
			return null;
		synchronized (lockObj) {
			int index = _q.size();
			// 定位,查找开始取数的位置
			for (int i = 0; i < _q.size(); i++) {
				Comment
				
				e = _q.get(i);
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
}
