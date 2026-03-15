package com.yfzx.service.db;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.StockConstants;
import com.stock.common.event.NotifyEvent;
import com.stock.common.model.Company;
import com.stock.common.model.USubject;
import com.stock.common.model.snn.EConst;
import com.stock.common.model.stockgame.StockDelegation;
import com.stock.common.msg.DelegateOrderMsg;
import com.stock.common.util.StockUtil;
import com.yfzx.service.stockgame.StockDelegationService;
import com.yz.mycore.lcs.enter.LCEnter;
import com.yz.mycore.msg.ClientEventCenter;
import com.yz.mycore.msg.message.IMessage;

/**
 * 委托下单服务类
 *
 * @author：杨真
 * @date：2014年9月3日
 */
public class DelegateOrderService {

	private static DelegateOrderService instance = new DelegateOrderService();
	Logger log = LoggerFactory.getLogger(this.getClass());

	private DelegateOrderService() {

	}

	public static DelegateOrderService getInstance() {
		return instance;
	}

	public void notifyTheEvent(IMessage im) {
		NotifyEvent ne = new NotifyEvent();
		ne.setHType(EConst.EVENT_7);
		ne.setMsg(im);
		ClientEventCenter.getInstance().putEvent2ChildQueue(EConst.EVENT_7,ne);
	}

	public void notifyDelegate(String companycode,
			Double cp) {
		DelegateOrderMsg dom = new DelegateOrderMsg(companycode,cp);
		notifyTheEvent(dom);
	}
	/**
	 * 委托
	 * @param dg
	 */
	public void delegate(StockDelegation dg)
	{
		Company c = CompanyService.getInstance().getCompanyByCodeFromCache(dg.getStockCode());
		if(c==null||dg.getDelegationPrice()==0)
		{
			log.error("delegate parameter error!");
			return;
		}
		if(dg.getBargainType() == 0)
		{
			//直接成交
			if(dg.getDelegationPrice()>=c.getC())
			{
				//如果交易失败，则做为挂单处理
				if(!StockDelegationService.getInstance().dealSuccessStockDelegation(dg))
				{
					hangupDelegationOrder(dg);
				}
			}
			else
			{
				//做为挂单处理
				hangupDelegationOrder(dg);
			}
		}
		else
		{
			//直接成交
			if(dg.getDelegationPrice()<=c.getC())
			{
				//如果交易失败，则做为挂单处理
				if(!StockDelegationService.getInstance().dealSuccessStockDelegation(dg))
				{
					hangupDelegationOrder(dg);
				}
			}
			else
			{
				//做为挂单处理
				hangupDelegationOrder(dg);
			}
		}


	}
	/**
	 * 撤单
	 * @param companycode
	 * @param id
	 */
	public void clearHangupDelegationOrder(String companycode,Long id)
	{
		String ckn = StockUtil.getDelegateCacheName(companycode);
		Map<Long,StockDelegation> sdm = LCEnter.getInstance().get(companycode, ckn);
		if(sdm!=null)
		{
			sdm.remove(id);
		}
	}
	//做为挂单处理
	private void hangupDelegationOrder(StockDelegation dg) {
		String ckn = StockUtil.getDelegateCacheName(dg.getStockCode());
		Map<Long,StockDelegation> sdm = LCEnter.getInstance().get(dg.getStockCode(), ckn);
		if(sdm==null)
		{
			log.error("delegate_queue_not_init!");
			return;
		}
		sdm.put(dg.getId(), dg);

	}

	/**
	 * 是否有匹配成功的委托单
	 * @param dom
	 */
	public void findCanOrder(DelegateOrderMsg dom)
	{
		String ckn = StockUtil.getDelegateCacheName(dom.getCompanycode());
		Map<Long,StockDelegation> sdm = LCEnter.getInstance().get(dom.getCompanycode(), ckn);
		if(sdm==null)
		{
			log.error("delegate List not init!");
			return;
		}
		Company c = CompanyService.getInstance().getCompanyByCodeFromCache(dom.getCompanycode());
		//上一个时刻的价格
		Double prePrice = c.getAttr("_pre_price");
		if(prePrice==null)
			prePrice = c.getZs();
		if(prePrice==null || prePrice.doubleValue() == 0)
			return;

		Iterator<Long> iter = sdm.keySet().iterator();
		while(iter.hasNext())
		{
			Long sdid = iter.next();
			StockDelegation sd = sdm.get(sdid);
			if(sd.getEndTime().getTime()<=System.currentTimeMillis())
			{

				if(prePrice>dom.getCp())
				{
					//股价下跌，则如果代理价在成交区间，则成交
					if(sd.getDelegationPrice()>=dom.getCp()&&sd.getDelegationPrice()<prePrice)
					{
						//下单
						boolean r = StockDelegationService.getInstance().dealSuccessStockDelegation(sd);
						if(r)
						{
							sdm.remove(sdid);
						} else {
							log.info("findCanOrder4:  " + r);
						}
					}

				}
				else
				{
					//股价上涨
					if(sd.getDelegationPrice()<=dom.getCp()&&sd.getDelegationPrice()>prePrice)
					{
						//下单
						boolean r = StockDelegationService.getInstance().dealSuccessStockDelegation(sd);
						if(r)
						{
							sdm.remove(sdid);
						} else {
							log.info("findCanOrder5:  " + r);
						}
					}
				}

			}
			else
			{
				//过期单删除掉
				sdm.remove(sdid);
			}

		}

	}


	public void initDelegateOrderList()
	{
		List<USubject> usl = USubjectService.getInstance().getUSubjectListByType(StockConstants.SUBJECT_TYPE_0);
		for(USubject us:usl)
		{
			String cacheName = StockUtil.getDelegateCacheName(us.getUidentify());
			Map<Long,StockDelegation> sdm = LCEnter.getInstance().get(us.getUidentify(), cacheName);
			//未初始化的，放一个空的，防止并发操作
			if(sdm==null)
			{
				sdm = new ConcurrentHashMap<Long,StockDelegation>();
				LCEnter.getInstance().put(us.getUidentify(), sdm, cacheName);
			}
		}

	}

}
