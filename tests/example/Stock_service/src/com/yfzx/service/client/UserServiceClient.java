package com.yfzx.service.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.AAAConstants;
import com.stock.common.model.msgpush.MobileToken;
import com.stock.common.model.product.Order;
import com.stock.common.model.router.Dbrouter;
import com.stock.common.model.share.UserExt;
import com.stock.common.model.stockgame.StockDelegation;
import com.stock.common.model.stockgame.StockEquity;
import com.stock.common.model.stockgame.StockRevenue;
import com.stock.common.model.stockgame.StockSnapshoot;
import com.stock.common.model.stockgame.StockTransaction;
import com.stock.common.model.user.Members;
import com.stock.common.model.user.StockSeq;
import com.stock.common.model.user.ThirdUser;
import com.stock.common.service.IUserService;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.yfzx.yas.YASFactory;
import com.yfzx.yas.router.ISelectRouter;
import com.yfzx.yas.router.RouterCenter;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;

/**
 * 用户服务客户端
 *
 * @author：杨真
 * @date：2014-6-2
 */
public class UserServiceClient implements IUserService{

	static Map<String,Dbrouter> _routerPool = new ConcurrentHashMap<String,Dbrouter>();
	static Logger logger = LoggerFactory.getLogger(RouterCenter.class);
	private static UserServiceClient instance = new UserServiceClient();
	static String iserviceName = "IUserService";
	public UserServiceClient() {

	}

	public static UserServiceClient getInstance() {
		return instance;
	}
	static
	{
		initRouter();
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh(){

			public void refresh() {
				initRouter();

			}

		});
	}
	private static void reinitRouter()
	{
		List<Dbrouter> rl = RouterCenter.getInstance().getRouter(
				iserviceName);
		if(rl!=null&&_routerPool.keySet().size()!=rl.size())
			initRouter();
	}
	private static void initRouter() {
		try {
			// TODO Auto-generated method stub
			List<Dbrouter> rl = RouterCenter.getInstance().getRouter(
					iserviceName);
			if(rl==null)
			{
				logger.error("not found  roueter !");
				return ;
			}
			for (Dbrouter dr : rl) {
				_routerPool.put(getKey(dr.getSip(), dr.getSport()), dr);
			}
		} catch (Exception e) {
			logger.error("init roueter failed!",e);
		}
	}
	private static String getKey(String sip, int sport) {
		// TODO Auto-generated method stub
		return sip+"^"+sport;
	}
	public static void main(String[] args) {
//		104469
		int index = StockUtil.getUserIndex("104469")%2;
		System.out.println(index);

	}
	IUserService getService()
	{
		reinitRouter();
		return YASFactory.getService(IUserService.class,new ISelectRouter(){

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return "SR_"+iserviceName;
			}
			//根据缓存名来路由，不同的缓存可能放在不同的服务器上
			public Dbrouter selectRouter(String methodName, Object[] args) {
				Dbrouter dr = null;
				try {
					if(_routerPool.keySet().size()==0) return null;
					//k格式：000002.sz
					String k = args[0].toString();
					if (StringUtil.isEmpty(k))
						return null;
					String[] ka = k.toString().split("\\^");
					//路由key,以公司编码路由，这样同一公司的数据只会存储在一个缓存服务器上
					String rk = ka[0];

					//获取提供这个服务又哪些服务器 192.168.1.102:8855^192.168.1.103:8855^192.168.1.104:8855
					String sips = ConfigCenterFactory.getString(
							AAAConstants.SERVER_IP_LIST,
							"192.168.1.103:7777");

					String[] ips = sips.split("\\^");
					int dcssNums = ips.length;
					//根据用户uid或者username求余 得到
					//不建议客户端感知后端使用每台DCSS有多少个cache
					int index = StockUtil.getUserIndex(rk)%dcssNums; //StockUtil.getUExtTableIndex(rk)%ips.length;
					String selectIp =  ips[index];
					String ip = selectIp.split(":")[0];
					int port = Integer.valueOf(selectIp.split(":")[1]);
					dr = _routerPool.get(getKey(ip, port));
				} catch (Exception e) {
					logger.error("select router failed!",e);
				}
				return dr;
			}

		});
	}

	public Members getMemberByUserName(String username, String password)
	{
		return getService().getMemberByUserName(username,password);
	}

	public Members getMemberByAccount(String username) {
		return getService().getMemberByAccount(username);
	}

	public boolean removeMemberByUserName(String username) {
		return getService().removeMemberByUserName(username);
	}

	public Long getUidByNickname(String nickname){
		return getService().getUidByNickname(nickname);
	}

	public void saveUidByNickname(String nickname,long uid){
		getService().saveUidByNickname(nickname, uid);
	}

	@Override
	public void saveMembers(String username,Members members) {
		getService().saveMembers(username, members);
	}

//	@Override
//	public void saveMembersByAccount(String account,Members members) {
//		getService().saveMembersByAccount(account, members);
//	}

	@Override
	public UserExt getUserExtByUid(long uid) {
		return getService().getUserExtByUid(uid);
	}

	@Override
	public void updateUserExt(long uid, Map<String, Object> map) {
		getService().updateUserExt(uid, map);
	}

	public List<UserExt> getUserExtList(List<Long> uidList) {
		String sips = ConfigCenterFactory.getString(
				AAAConstants.SERVER_IP_LIST,
				"192.168.1.103:7777");

		String[] ips = sips.split("\\^");
		List<UserExt> ueList = new ArrayList<UserExt>();

		//将uid分组，然后去对应的DCSS取数据
		Map<Integer,List<Long>> uidListMap = new HashMap<Integer,List<Long>>();
		for(int i=0;i<uidList.size();i++){
			Long uid = uidList.get(i);
			int index =  StockUtil.getUserIndex(String.valueOf(uid))%ips.length;
			//Math.abs(String.valueOf(uid).hashCode())%ips.length;
			if( uidListMap.containsKey(index) == false ){
				uidListMap.put(index, new ArrayList<Long>());
			}
			List<Long> uidListTemp = uidListMap.get(index);
			uidListTemp.add(uid);
		}

		Iterator<Entry<Integer,List<Long>>> iter = uidListMap.entrySet().iterator();
		while (iter.hasNext()) {
			List<Long> uidListTemp = iter.next().getValue();
			if(uidListTemp!= null && uidListTemp.size() > 0){
				ueList.addAll(getService().getUserExtList(uidListTemp.get(0),uidListTemp));
			}
		}
		return ueList;
	}
	@Override
	public List<UserExt> getUserExtList(long uid,List<Long> uidList){
		return getService().getUserExtList(uid,uidList);
	}

	public List<Order> getOrderList(long uid){
		return getService().getOrderList(uid);
	}

	public boolean addOrder(long uid,Order order){
		return getService().addOrder(uid, order);
	}

	public StockSeq getStockSeq(long uid) {
		StockSeq s = null;
		try {
			s = getService().getStockSeq(uid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}

	public boolean addStockSeq(long uid, String stockcode, Date updateTime) {
		boolean r = false;
		try {
			r = getService().addStockSeq(uid,stockcode,updateTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}
	public boolean addStockSeq(long uid, StockSeq stockSeq) {
		boolean r = false;
		try {
			r = getService().addStockSeq(uid, stockSeq);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}

	public boolean updateStockSeq(long uid, String stockcodes, Date updateTime) {
		boolean r = false;
		try {
			r = getService().updateStockSeq(uid,stockcodes,updateTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}
	public boolean delStockSeq(long uid, String stockcode, Date updateTime) {
		boolean r = false;
		try {
			r = getService().delStockSeq(uid, stockcode, updateTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}

	public boolean batchDelStockSeq(long uid, String stockcodes, Date updateTime) {
		boolean r = false;
		try {
			r = getService().batchDelStockSeq(uid, stockcodes, updateTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return r;
	}

	@Override
	public List<Long> getBefollowListOnlineLocalFromRemoteCache(Long uid) {
		// TODO Auto-generated method stub
		return getService().getBefollowListOnlineLocalFromRemoteCache(uid);
	}
	
	@Override
	public List<StockRevenue> getStockRevenueByUidList(Long uid, List<Long> uidList,
			int type,boolean isOrderTime) {
		List<StockRevenue> srList = null;
		try {
			srList = getService().getStockRevenueByUidList(uid, uidList,type, isOrderTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return srList;
	}

	public StockRevenue getStockRevenueByUid(Long uid) {
		StockRevenue s = null;
		try {
			s = getService().getStockRevenueByUid(uid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return s;
	}

	public boolean saveStockRevenue(Long uid, StockRevenue sr) {
		try {
			return getService().saveStockRevenue(uid, sr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean delStockRevenue(Long uid, StockRevenue sr) {
		try {
			return getService().delStockRevenue(uid, sr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public StockSnapshoot getStockSnapshootByUid(Long uid) {
		try {
			return getService().getStockSnapshootByUid(uid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean saveStockSnapshoot(Long uid, StockSnapshoot snapshoot) {
		try {
			return getService().saveStockSnapshoot(uid, snapshoot);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public StockEquity getStockEquityByCodeUid(Long uid, String companyCode) {
		return getService().getStockEquityByCodeUid(uid, companyCode);
	}

	public boolean saveStockEquity(Long uid, StockEquity se) {
		try {
			return getService().saveStockEquity(uid, se);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean delStockEquity(Long uid, StockEquity se) {
		try {
			return getService().delStockEquity(uid, se);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<StockEquity> getStockEquityPagingList(Long uid, int offset, int limit) {
		try {
			return getService().getStockEquityPagingList(uid, offset, limit);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean saveStockTransaction(Long uid, StockTransaction st) {
		try {
			return getService().saveStockTransaction(uid, st);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<StockTransaction> getStockTransactionListByPage(Long uid, Integer offset, Integer limit) {
		try {
			return getService().getStockTransactionListByPage(uid, offset, limit);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean saveTopRankStockRevenueList(int type, List<StockRevenue> seList,boolean isOrderTime) {
		try {
			return getService().saveTopRankStockRevenueList(type, seList,isOrderTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<StockRevenue> getTopRankStockRevenueByType(int type, int offset, int limit,boolean isOrderTime) {
		try {
			return getService().getTopRankStockRevenueByType(type, offset, limit,isOrderTime);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean saveMobileToken(Long uid, MobileToken mobileToken) {
		try {
			return getService().saveMobileToken(uid, mobileToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean removeMobileToken(Long uid, MobileToken mobileToken) {
		try {
			return getService().removeMobileToken(uid, mobileToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean removeMobileTokensByUid(Long uid) {
		try {
			return getService().removeMobileTokensByUid(uid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public Set<MobileToken> getMobileTokenSet(Long uid) {
		try {
			return getService().getMobileTokenSet(uid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public boolean updateMobileTokenStateByUidToken(Long uid, String token, int appState) {
		try {
			return getService().updateMobileTokenStateByUidToken(uid, token, appState);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public void deleteMobileToken(Long uid, String token) {
		getService().deleteMobileToken(uid, token);
	}

	public boolean saveStockDelegation(Long uid, StockDelegation sd) {
		try {
			return getService().saveStockDelegation(uid, sd);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean removeStockDelegation(Long uid, Long id) {
		try {
			return getService().removeStockDelegation(uid, id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public List<StockDelegation> getStockDelegationList(Long uid) {
		try {
			return getService().getStockDelegationList(uid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public Set<Long> getIOSMobileTokenMemberUids(String key) {
		try {
			return getService().getIOSMobileTokenMemberUids(key);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getIOSMobileTokenMemberUids: " + e);
		}
		return null;
	}

	public Set<String> getAsyncTasks(String key) {
		try {
			return getService().getAsyncTasks(key);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("getAsyncTasks: " + e);
		}
		return null;
	}

	public void saveAsyncTasks(String key, String item) {
		try {
			getService().saveAsyncTasks(key, item);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("saveAsyncTasks: " + e);
		}
	}

	public void removeAsyncTasks(String key, Set<String> rsets) {
		try {
			getService().removeAsyncTasks(key, rsets);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("removeAsyncTasks: " + e);
		}
	}

	public ThirdUser getThirdUser(String openId, int platform) {
		try {
			return getService().getThirdUser(openId, platform);
		} catch (Exception e) {
			logger.error("getThirdUser: " + e);
		}
		return null;
	}

	public boolean saveThirdUser(String openId, ThirdUser user) {
		try {
			return getService().saveThirdUser(openId, user);
		} catch (Exception e) {
			logger.error("saveThirdUser: " + e);
			return false;
		}
	}
	public boolean delThirdUser(String openId) {
		try {
			return getService().delThirdUser(openId);
		} catch (Exception e) {
			logger.error("delThirdUser: " + e);
			return false;
		}
	}

	public boolean delMemberCache(String account) {
		try {
			return getService().delMemberCache(account);
		} catch (Exception e) {
			logger.error("delMemberCache: " + e);
			return false;
		}

	}

	
}
