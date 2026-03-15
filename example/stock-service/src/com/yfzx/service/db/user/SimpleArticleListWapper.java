//package com.yfzx.service.db.user;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import com.stock.common.model.share.SimpleArticle;
///**
// * 消息组包装类，带过期时间
// * 由SimpleArticle[收藏]转成 SimpleArticle @author tangbinqi 20140819
// * @author：杨真 
// * @date：2014-7-26
// */
//public class SimpleArticleListWapper implements Serializable {
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = -7824414181229037973L;
//	Logger log = LoggerFactory.getLogger(SimpleArticleListWapper.class);
//	List<SimpleArticle> _q;
//	String key;
//	int qsize = 500;
//
//	public SimpleArticleListWapper() {
//		_q = new ArrayList<SimpleArticle>(qsize);
//	}
//
//	public SimpleArticleListWapper(int qsize) {
//		_q = new ArrayList<SimpleArticle>(qsize);
//	}
//	public List<SimpleArticle> getQueue() {
//		if (_q == null) {
//			_q = new ArrayList<SimpleArticle>(qsize);
//		}
//		return _q;
//
//	}
//
//	public SimpleArticleListWapper(String key) {
//		this.key = key;
//	}
//
//	public void put(SimpleArticle item) {
//		synchronized(this)
//		{
//			//如果已满，就把尾部移掉
//			if(_q.size()==qsize)
//				_q.remove(_q.size()-1);
//			//加到头部
//			_q.add(0, item);
//		}
//	}
//	
//	/**
//	 * 本方法 主要用于初始加载 tbq
//	 * @param itemList 倒序数组 0元素时间最近
//	 */
//	public void putAll(List<SimpleArticle> itemList){
//		synchronized(this)
//		{
//			//加到头部
//			_q.addAll(0,itemList);
//			//去除超出的元素 
//			if(_q.size() > qsize){
//				//倒序删除，不会触发 List对象的System.arraycopy过程
//				for(int i=_q.size()-1;i>=0;i--){ 
//				    if(i >= qsize){
//				    	_q.remove(i);
//				    }
//				}
////				_q.removeAll(_q.subList(qsize, _q.size()));
//			}
//		}
//	}
//
//
//	public String getKey() {
//		return key;
//	}
//
//	public void setKey(String key) {
//		this.key = key;
//	}
//
//	/**
//	 * 从前往后，按时间降序排列(根据时间 取time之后的博文记录)
//	 * @param time
//	 * @param start
//	 * @param limit
//	 * @return
//	 */
//	public List<SimpleArticle> getSimpleArticleList(long time, int start, int limit)
//	{
//		List<SimpleArticle> el = new ArrayList<SimpleArticle>();
//		if(_q.size()==0)
//			return null;
//		for(int i=0;i<_q.size();i++)
//		{
//			SimpleArticle e = _q.get(i);
//			if(e!=null&&e.getTime()>=time&&i>=start&&i<(start+limit))
//				el.add(e);
//			else
//			{
//				//由于是按时间排好序的，所以出以下情况，就可以中止循环，减少不必要的循环
//				if(e.getTime()<time||i>=(start+limit))
//					break;
//			}
//		}
//		return el;
//	}
//	/**
//	 * 从前往后，按时间降序排列(根据时间 取time之前的博文记录)
//	 * @param time
//	 * @param start
//	 * @param limit
//	 * @return
//	 */
//	public List<SimpleArticle> getSimpleArticleListBeforTime(long time, int start, int limit)
//	{
//		List<SimpleArticle> el = new ArrayList<SimpleArticle>();
//		if(_q.size()==0)
//			return null;
//		for(int i=0;i<_q.size();i++)
//		{
//			SimpleArticle e = _q.get(i);
//			if(e!=null&&e.getTime()<=time&&i>=start&&i<(start+limit))
//				el.add(e);
//			else
//			{
//				//由于是按时间排好序的，所以出以下情况，就可以中止循环，减少不必要的循环
//				if(i>=(start+limit))
//					break;
//			}
//		}
//		return el;
//	}
//}
