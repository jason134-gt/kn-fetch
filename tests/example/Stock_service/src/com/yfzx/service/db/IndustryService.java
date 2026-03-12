package com.yfzx.service.db;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.struts2.json.JSONUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.constants.SCache;
import com.stock.common.constants.SExt;
import com.stock.common.constants.StockConstants;
import com.stock.common.constants.StockRegex;
import com.stock.common.model.Cfirule;
import com.stock.common.model.Company;
import com.stock.common.model.Dictionary;
import com.stock.common.model.IndexMessage;
import com.stock.common.model.Industry;
import com.stock.common.model.SmallIndustry;
import com.stock.common.model.company.Stock;
import com.stock.common.msg.Message;
import com.stock.common.msg.common.DataItem;
import com.stock.common.util.DateUtil;
import com.stock.common.util.SMathUtil;
import com.stock.common.util.StockUtil;
import com.stock.common.util.StringUtil;
import com.stock.common.util.TreeNode;
import com.yfzx.dc.function.GetIndustryValue;
import com.yfzx.service.StockCenter;
import com.yfzx.service.agent.IndexValueAgent;
import com.yfzx.service.cache.IndustryExtCacheService;
import com.yfzx.service.client.DcssIndustryExtIndexServiceClient;
import com.yfzx.service.factory.SMsgFactory;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.yz.mycore.lcs.enter.LCEnter;

public class IndustryService {

	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();
	private static IndustryService instance = new IndustryService();
	private static HashMap<String, String> industryCodeMapping = new HashMap<String, String>();
	public static String root = "-1";
	public static String root_name = "所有行业";
	public static String root_uidentify = "所有行业";
	public static int zjs=0;
	public static int yfzx=1;
	private static Map<String, Set<String>> _comapnyTagSet = new HashMap<String, Set<String>>();
	private static HashMap<String, TreeNode> dmflTree = null;
	Logger log = LoggerFactory.getLogger(this.getClass());
	static {
		// 第一个值为证监会二级行业，第二个值为国家二级行业
		industryCodeMapping.put("A01", "A01");// 农业
		industryCodeMapping.put("A03", "A02");// 林业
		industryCodeMapping.put("A05", "A03");// 畜牧业
		industryCodeMapping.put("A07", "A04");// 渔业
		industryCodeMapping.put("A09", "A05");// 农、林、牧、渔服务业

		industryCodeMapping.put("B01", "B06");// 煤炭开采和洗选业
		industryCodeMapping.put("B03", "B07");// 石油和天然气开采业
		industryCodeMapping.put("B05", "B08");// 黑色金属矿采选业
		industryCodeMapping.put("B07", "B09");// 有色金属矿采选业
		industryCodeMapping.put("B09", "B10");// 非金属矿采选业
		industryCodeMapping.put("B49", "B11");// 其他采矿业
		industryCodeMapping.put("B50", "");// 采掘服务业 缺少

		industryCodeMapping.put("C01", "C13");// 食品加工业
		industryCodeMapping.put("C03", "C14");// 食品制造业
		industryCodeMapping.put("C05", "C15");// 饮料制造业
												// 烟草制品业 缺少
		industryCodeMapping.put("C11", "C17");// 纺织业
		industryCodeMapping.put("C13", "C18");// 服装及其他纤维制品制造业
		industryCodeMapping.put("C14", "C19");// 皮革、毛皮、羽绒及制品制造业
		industryCodeMapping.put("C21", "C20");// 木材加工及木、竹、藤、棕、草制品业
		industryCodeMapping.put("C25", "C21");// 家具制造业
		industryCodeMapping.put("C31", "C22");// 造纸及纸制品业
		industryCodeMapping.put("C35", "C23");// 印刷业
		industryCodeMapping.put("C37", "C24");// 文教体育用品制造业
		industryCodeMapping.put("C41", "C25");// 石油加工及炼焦业
		industryCodeMapping.put("C43", "C26");// 化学原料及化学制品制造业
		industryCodeMapping.put("C81", "C27");// 医药制造业
		industryCodeMapping.put("C85", "C27");// 生物制品业
		industryCodeMapping.put("C47", "C28");// 化学纤维制造业
		industryCodeMapping.put("C48", "C29");// 橡胶制品业
		industryCodeMapping.put("C49", "C30");// 塑料制品业

		industryCodeMapping.put("C51", "");// 电子元器件制造业
		industryCodeMapping.put("C55", "");// 日用电子器具制造业
		industryCodeMapping.put("C57", "");// 其他电子设备制造业
		industryCodeMapping.put("C59", "");// 电子设备修理业

		industryCodeMapping.put("C61", "C31");// 非金属矿物制品业
		industryCodeMapping.put("C65", "C32");// 黑色金属冶炼及压延加工业
		industryCodeMapping.put("C67", "C33");// 有色金属冶炼及压延加工业
		industryCodeMapping.put("C69", "C34");// 金属制品业

		industryCodeMapping.put("C71", "C35");// 通用设备制造业
		industryCodeMapping.put("C73", "C36");// 专用设备制造业
		industryCodeMapping.put("C75", "C37");// 交通运输设备制造业
		industryCodeMapping.put("C76", "C39");// 电器机械及器材制造业
		industryCodeMapping.put("G81", "C40");// 通信设备、计算机及其他电子设备制造业
		industryCodeMapping.put("C78", "C41");// 仪器仪表及文化、办公用机械制造业
		industryCodeMapping.put("C99", "C42");// 工艺品及其他制造业

		industryCodeMapping.put("D01", "D44");// 电力、蒸汽、热水的生产和供应业
		industryCodeMapping.put("D03", "D45");// 燃气生产和供应业
		industryCodeMapping.put("D05", "D46");// 水的生产和供应业

		industryCodeMapping.put("E01", "E47");// 房屋和土木工程建筑业
		industryCodeMapping.put("E05", "E49");// 装修装饰业

		industryCodeMapping.put("F01", "F51");// 铁路运输业
		industryCodeMapping.put("F03", "F52");// 道路运输业
		industryCodeMapping.put("F05", "F56");// 管道运输业
		industryCodeMapping.put("F07", "F54");// 水上运输业
		industryCodeMapping.put("F09", "F55");// 航空运输业
		industryCodeMapping.put("F19", "F57");// 其他交通运输业
		industryCodeMapping.put("F21", "F58");// 仓储业

		industryCodeMapping.put("G85", "G60");// 电信和其他信息传输服务业
		industryCodeMapping.put("G87", "G61;G62");// 计算机应用服务业

		industryCodeMapping.put("H01", "H631;H632;H633;H634;H635");// 食品、饮料、烟草和家庭用品批发业
		industryCodeMapping.put("H03", "H636;H637;H639");// 能源、材料和机械电子设备批发业
		industryCodeMapping.put("H09", "H639");// 其他批发业
		industryCodeMapping.put("H11", "H65");// 零售业
		industryCodeMapping.put("H21", "H638");// 商业经纪与代理业

		industryCodeMapping.put("I01", "J68");// 银行业
		industryCodeMapping.put("I11", "J70");// 保险业
		industryCodeMapping.put("I21", "J69");// 证券、期货业
		industryCodeMapping.put("I31", "J71");// 金融信托业
		industryCodeMapping.put("I99", "J71");// 其他金融业

		industryCodeMapping.put("J01", "K721");// 房地产开发经营
		industryCodeMapping.put("J05", "K722;K729");// 房地产管理业
		industryCodeMapping.put("J09", "K723");// 房地产中介服务业

		industryCodeMapping.put("K01", "F53");// 公共设施服务业
		industryCodeMapping.put("K10", "F59");// 邮政服务业
		industryCodeMapping.put("K20", "F59");// 商务服务业
		industryCodeMapping.put("K30", "I67");// 餐饮业
		industryCodeMapping.put("K32", "I66");// 旅馆业
		industryCodeMapping.put("K34", "L748;I661");// 旅行业 旅行社，旅游宾馆
		industryCodeMapping.put("K36", "R93");// 娱乐服务业
		industryCodeMapping.put("K37", "Q85;Q86;Q87");// 卫生、保健、护理服务业
		industryCodeMapping.put("K39", "L73");// 租赁业

		industryCodeMapping.put("L01", "R88");// 出版业
		industryCodeMapping.put("L05", "R894");// 声像业
		industryCodeMapping.put("L10", "R89");// 广播电影电视业
		industryCodeMapping.put("L15", "R90");// 文化艺术业
		industryCodeMapping.put("L99", "R90");// 文化艺术业

	}

	private IndustryService() {

	}

	public static IndustryService getInstance() {
		return instance;
	}

	public String getYfzxAllIndustryJsonData() {
		return LCEnter.getInstance().get(SCache.YFZX_ALL_INDUSTRY_JSON, SCache.CACHE_NAME_INDUSTRY);
	}
	public List<Industry> getAllIndustry() {
		RequestMessage req = DAFFactory.buildRequest("getAllIndustry",
				StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null) {
			return null;
		}
		return (List<Industry>) o;
	}

	/**
	 * 取某个公司所属的子行业
	 * @param companycode
	 */
	public String getIndustryByCompany(String companycode) {
		CompanyService cs = CompanyService.getInstance();
		Company c = cs.getCompanyByCode(companycode);
		return c.getF042v();
	}
	/**
	 * 唐斌奇 2012-07-16 增加 获取证监会行业标准里的所有行业 China Securities Regulatory Commission
	 * 
	 * @return
	 */
	public List<Industry> getAllIndustryCSRC() {
		RequestMessage req = DAFFactory.buildRequest("getAllIndustryCSRC",
				StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null) {
			return null;
		}
		return (List<Industry>) o;
	}

	/**
	 * 获取全部“国民经济行业分类与代码” 唐斌奇
	 * 
	 * @return
	 */
	public List<Industry> getAllIndustryDMFL() {
		RequestMessage req = DAFFactory.buildRequest("getAllIndustryDMFL",
				StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null) {
			return null;
		}
		return (List<Industry>) o;
	}
	
	public List<Industry> getAllIndustrySW() {
		RequestMessage req = DAFFactory.buildRequest("getAllIndustrySW",
				StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null) {
			return null;
		}
		return (List<Industry>) o;
	}

	/**
	 * replace“国民经济行业分类与代码” 主要是对父Code
	 */
	public void reBuildDMFL() {
		List<Industry> dmflArr = this.getAllIndustryDMFL();
		this.rebuildIndArrWithParent(dmflArr);
		for (Industry dmfl : dmflArr) {
			repalceDMFL(dmfl);
		}
	}

	private void repalceDMFL(Industry dmfl) {
		RequestMessage req = DAFFactory.buildRequest("repalceIndustryDMFL",
				dmfl, StockConstants.common);
		pLayerEnter.insert(req);
	}
	
	/**
	 * 重新构造申万的行业，增加父ID
	 */
	public void reBuildSW() {
		List<Industry> swArr = this.getAllIndustrySW();
		this.rebuildIndArrWithParent(swArr);
		for (Industry sw : swArr) {
			repalceSW(sw);
		}
	}

	private void repalceSW(Industry sw) {
		RequestMessage req = DAFFactory.buildRequest("repalceIndustrySW",
				sw, StockConstants.common);
		pLayerEnter.insert(req);
	}

	/**
	 * @param industryName
	 *            一级分类名称 （国家和证监会一致）
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List getTreeNode2Arr(String industryCode) {
		if (dmflTree == null)
			dmflTree = getIndustryDMFLTree();
		String industryCodeTop2 = industryCode.substring(0, 3);
		String dmflCodes = industryCodeMapping.get(industryCodeTop2);
		if (dmflCodes == null)
			return null;
		String[] dmflCodeArr = dmflCodes.split(";");// 因为一个证监会行业可以对应多个国家经济行业分类
		List reArr = new ArrayList();
		for (String dmflCode : dmflCodeArr) {
			TreeNode node = dmflTree.get(dmflCode);// 根节点
			if (node == null)
				return null;
			reArr.addAll(node.getChildAndChild());
		}
		return reArr;
	}

	/**
	 * 得到“国民经济行业分类与代码”树
	 * 
	 * @return
	 */
	public HashMap<String, TreeNode> getIndustryDMFLTree() {
		List<Industry> indArr = getAllIndustryDMFL();
		return buildIndustryTreeByCode(indArr,0);
		//return null;
	}

	public TreeNode getRoot(HashMap<String, TreeNode> th) {
		return th.get(root);
	}

	/**
	 * 唐斌奇 2012-07-17 增加 获取证监会行业树
	 * 
	 * @return 树形的Map,可以用Map的get方法,也可以用树的递归遍历子Node来获取某个行业
	 */
	public HashMap<String, TreeNode> getIndustryCSRCTree() {

		List<Industry> indArr = getAllIndustryCSRC();
		return buildIndustryTreeByCode(indArr,zjs);

	}

	/**
	 * type 0:code的映射，1：名的映射
	 * 
	 * @param type
	 * @return
	 */
	public HashMap<String, TreeNode> getIndustryYFZXTreeFromCache(int type) {
		HashMap<String, TreeNode> rtm = null;
		if (type == 0) {
			rtm = LCEnter.getInstance().get(SCache.CACHE_KEY_INDUSTRY_YFZX_CODE,
					SCache.CACHE_NAME_INDUSTRY);
			if (rtm == null) {
				rtm = getIndustryYFZXTree();
				if (rtm != null)
					LCEnter.getInstance().put(
							SCache.CACHE_KEY_INDUSTRY_ZJS_CODE, rtm,
							SCache.CACHE_NAME_INDUSTRY);
			}
		}

		if (type == 1) {
			rtm = LCEnter.getInstance().get(SCache.CACHE_KEY_INDUSTRY_YFZX_NAME,
					SCache.CACHE_NAME_INDUSTRY);
			if (rtm == null) {
				rtm = getIndustryYFZXTreeNameMap();
				if (rtm != null)
					LCEnter.getInstance().put(
							SCache.CACHE_KEY_INDUSTRY_ZJS_NAME, rtm,
							SCache.CACHE_NAME_INDUSTRY);
			}
		}
		return rtm;

	}

	public HashMap<String, TreeNode> getIndustryYFZXTreeNameMap() {
		List<Industry> indArr = getAllIndustryYFZX();
		return buildIndustryTreeByName(indArr,yfzx);
	}

	/**
	 * type 0:code的映射，1：名的映射
	 * 
	 * @param type
	 * @return
	 */
	public HashMap<String, TreeNode> getIndustryCSRCTreeFromCache(int type) {
		HashMap<String, TreeNode> rtm = null;
		if (type == 0) {
			rtm = LCEnter.getInstance().get(SCache.CACHE_KEY_INDUSTRY_ZJS_CODE,
					SCache.CACHE_NAME_INDUSTRY);
			if (rtm == null) {
				rtm = getIndustryCSRCTree();
				if (rtm != null)
					LCEnter.getInstance().put(
							SCache.CACHE_KEY_INDUSTRY_ZJS_CODE, rtm,
							SCache.CACHE_NAME_INDUSTRY);
			}
		}

		if (type == 1) {
			rtm = LCEnter.getInstance().get(SCache.CACHE_KEY_INDUSTRY_ZJS_NAME,
					SCache.CACHE_NAME_INDUSTRY);
			if (rtm == null) {
				rtm = getIndustryCSRCTreeNameMap();
				if (rtm != null)
					LCEnter.getInstance().put(
							SCache.CACHE_KEY_INDUSTRY_ZJS_NAME, rtm,
							SCache.CACHE_NAME_INDUSTRY);
			}
		}
		return rtm;

	}
	/**
	 * 名的映射
	 * 
	 * @return
	 */
	public HashMap<String, TreeNode> getIndustryCSRCTreeNameMap() {

		List<Industry> indArr = getAllIndustryCSRC();
		return buildIndustryTreeByName(indArr,zjs);

	}

	private HashMap<String, TreeNode> buildIndustryTreeByName(
			List<Industry> indArr,int type) {
		HashMap<String, TreeNode> indTreeMap = new HashMap<String, TreeNode>();
		// 虚拟根目录
		Industry rootInd = new Industry();
		rootInd.setId(root);
		rootInd.getIndustryCode();
		rootInd.setName(root_name);
		TreeNode rootNode = new TreeNode(rootInd);
		indTreeMap.put(rootInd.getName(), rootNode);

		// 获取行业数据,自动构造parentCode，后期不需要时，可以优化
		// rebuildIndArrWithParent(indArr);

		// 将行业加载到树中和Map中
		for (Iterator<Industry> iter = indArr.iterator(); iter.hasNext();) {

			try {
				Industry industry = iter.next();
				TreeNode indNode = new TreeNode(industry);
				indTreeMap.put(industry.getName(), indNode);
				Industry pind = getIndustryByCode(industry.getParentCode(),type);
				if(pind==null) continue;
				String pk = pind.getName();
				TreeNode parentNode = indTreeMap.get(pk);
				//有可能父结点还未加入到map中--杨真修改
				if(parentNode==null) 
				{
					parentNode = new TreeNode(pind);
					indTreeMap.put(pk, parentNode);
				}
				else
				{
					Object o = parentNode.getReference();
					if(o!=null)
					{
						Industry ind = (Industry)o;
						int cdepth = ind.getDepth()+1;
						((Industry)indNode.getReference()).setDepth(cdepth);
					}
				}
				parentNode.addChildNode(indNode);
			} catch (Exception e) {
				log.error("build industry tree failed!",e);
			}
		}
		return indTreeMap;
	}
	
	/**
	 * 重构得到基础方法
	 * 
	 * @param indArr
	 * @return
	 */
	private HashMap<String, TreeNode> buildIndustryTreeByCode(
			List<Industry> indArr,int type) {
		HashMap<String, TreeNode> indTreeMap = new HashMap<String, TreeNode>();
		// 虚拟根目录
		Industry rootInd = new Industry();
		rootInd.setId(root);
		rootInd.setDepth(0);
		rootInd.getIndustryCode();
		rootInd.setName(root_name);
		TreeNode rootNode = new TreeNode(rootInd);
		indTreeMap.put(root, rootNode);

		// 获取行业数据,自动构造parentCode，后期不需要时，可以优化
		// rebuildIndArrWithParent(indArr);

		// 将行业加载到树中和Map中
		for (Iterator<Industry> iter = indArr.iterator(); iter.hasNext();) {

			Industry industry = iter.next();
			TreeNode indNode = new TreeNode(industry);
			indTreeMap.put(industry.getIndustryCode(), indNode);

			TreeNode parentNode = indTreeMap.get(industry.getParentCode());
			//有可能父结点还未加入到map中--杨真修改
			/*if(parentNode==null) 
			{
				Industry pind = getIndustryByCode(industry.getParentCode(),type);
				if(pind==null)
					continue;
				parentNode = new TreeNode(pind);
				indTreeMap.put(pind.getIndustryCode(), parentNode);
			}*/
			if(parentNode == null){//父节点不存在，则初始化时可能出错，存在"或者空格等问题，需要人工处理
				log.error("缺少父节点的行业=["+industry.getIndustryCode()+","+industry.getName()+"]");
				continue;
			}
			Object o = parentNode.getReference();
			if(o!=null)
			{
				Industry ind = (Industry)o;
				int cdepth = ind.getDepth()+1;
				((Industry)indNode.getReference()).setDepth(cdepth);
			}
			parentNode.addChildNode(indNode);
		}
		return indTreeMap;
	}

	private Industry getIndustryByCode(String industryCode, int type) {
		
		if(type==zjs)
			return LCEnter.getInstance().get(getZJSCacheKey(industryCode), SCache.CACHE_NAME_INDUSTRY);
		if(type==yfzx)
			return LCEnter.getInstance().get(getYFZXCacheKey(industryCode), SCache.CACHE_NAME_INDUSTRY);
		return null;
	}

private Industry getIndustryByName(String industryName, int type) {
		
		if(type==zjs)
			return LCEnter.getInstance().get(getZJSCacheKey(industryName), SCache.CACHE_NAME_INDUSTRY);
		if(type==yfzx)
			return LCEnter.getInstance().get(getYFZXCacheKey(industryName), SCache.CACHE_NAME_INDUSTRY);
		return null;
	}

	/**
	 * 唐斌奇 2012-07-18 增加 获取盈富在线行业树
	 * 
	 * @return 树形的Map,可以用Map的get方法,也可以用树的递归遍历子Node来获取某个行业
	 */
	public HashMap<String, TreeNode> getIndustryYFZXTree() {
		List<Industry> indArr = this.getAllIndustryYFZX();
		return buildIndustryTreeByCode(indArr,yfzx);
	}

	public HashMap<String, TreeNode> getIndustryYFZXTreeFromCache() {
		List<Industry> indArr = getAllIndustryYFZXFromCache();
		return buildIndustryTreeByCode(indArr,yfzx);
	}
	private List<Industry> getAllIndustryYFZXFromCache() {
		// TODO Auto-generated method stub
		return LCEnter.getInstance().get(SCache.CACHE_KEY_INDUSTRY_All_YFZX, SCache.CACHE_NAME_INDUSTRY);
	}

	/**
	 * 将证劵会行业或申银万国初始行业，重新构建出带父Code的行业 数据库已经存在parentCode时，不需要此方法
	 * 
	 * @param IndArr
	 */
	private void rebuildIndArrWithParent(List<Industry> IndArr) {

		for (Industry ind : IndArr) {
			String indCode = ind.getIndustryCode();
			String parentCode = root;
			int startIndex = 0;
			for (Industry ind2 : IndArr) {
				if (ind2.getId().equals(ind.getId()))
					continue;
				String checkParentCode = ind2.getIndustryCode();
				if (indCode.startsWith(checkParentCode)) {
					if (startIndex < checkParentCode.length()) {
						parentCode = checkParentCode;
						startIndex = checkParentCode.length();
					}
				}
			}

			ind.setParentCode(parentCode);

		}

	}

	public Industry getIndustryCSRCByCode(String industryCode) {
		return LCEnter.getInstance().get(getZJSCacheKey(industryCode), SCache.CACHE_NAME_INDUSTRY);
		
	}

	public Industry getIndustryYfzxByCode(String industryCode) {
		return LCEnter.getInstance().get(getYFZXCacheKey(industryCode), SCache.CACHE_NAME_INDUSTRY);
		
	}
	private Industry getIndustryCSRC(Industry industry) {
		String sqlMapKey = "selectCSRC";
		RequestMessage req = DAFFactory.buildRequest(
				industry.getIndustryCode(), sqlMapKey, industry,
				StockConstants.common);
		Object value = pLayerEnter.queryForObject(req);
		if (value == null) {
			return null;
		}
		return (Industry) value;
	}

	public Industry getIndustryCSRCByName(String name) {
		TreeNode tn = getIndustryCSRCTreeFromCache(1).get(name);
		if (tn == null)
			return null;
		Object o = tn.getReference();
		if (o == null)
			return null;
		return (Industry) o;
	}

	public Industry getIndustryYFZXByName(String name) {
		return getIndustryByName(name,yfzx);
	}
	
	public List<Industry> getAllIndustryYFZX() {
		RequestMessage req = DAFFactory.buildRequest("getAllIndustryYFZX",
				StockConstants.common);
		Object o = pLayerEnter.queryForList(req);
		if (o == null) {
			return null;
		}
		return (List<Industry>) o;
	}

	/**
	 * 计算最大值，最小值，平均值
	 */

	// public void computeMaxMinAvgByTag(String tag) {
	// // 取所有要计算的指标
	// List<Dictionary> dl = DictService.getInstance().getAllDictionaryList();
	// Set<String> companycodeSet = getCompanyCodeSet(tag);
	// for (Dictionary d : dl) {
	// if (!StringUtil.isEmpty(d.getColumnChiName()) && d.getType() >= 0
	// && d.getType() <= 6) {
	// computeMaxMinAvgOneIndex(d, tag, companycodeSet);
	// }
	//
	// }
	//
	// }

	// @SuppressWarnings({ "rawtypes", "unchecked" })
	// private void computeMaxMinAvgOneIndex(Dictionary d, String tag,
	// Set<String> companycodeSet) {
	// try {
	// String sTime = "1980-6-30";
	// String eTime = StockUtil.getApproPeriod(new Date());
	// Date sd = DateUtil.format(sTime, DateUtil.YYYYMMDD);
	// Date ed = DateUtil.format(eTime, DateUtil.YYYYMMDD);
	// // 如果起始时间,小于结束时间
	// while (sd.compareTo(ed) <= 0) {
	//
	// String dsetkey = d.getIndexCode()+"_"+sTime;
	// //如果此指标的这个时间没有数据则不进行下面的处理
	// if(_noDataSet.contains(dsetkey))
	// {
	// sTime = StockUtil.getNextTime(sTime, 3);
	// sd = DateUtil.format(sTime, DateUtil.YYYYMMDD);
	// continue ;
	// }
	//
	//
	//
	// List il = null;
	// if(d.getType()==6)
	// {
	// il = IndexService.getInstance().getIndexListByTimeFrom(
	// d.getTableName(), d.getColumnName(), sTime);
	// }
	// else
	// {
	// il = IndexService.getInstance().getIndexListByTime(
	// d.getTableName(), d.getColumnName(), sTime);
	// }
	// if (il == null || il.size() == 0) {
	// // 取下一个时间点
	// sTime = StockUtil.getNextTime(sTime, 3);
	// sd = DateUtil.format(sTime, DateUtil.YYYYMMDD);
	// _noDataSet.add(dsetkey);
	// continue;
	// }
	//
	// List ilist = buildNewindexlist(il, companycodeSet);
	// if (ilist == null || ilist.size() == 0) {
	// // 取下一个时间点
	// sTime = StockUtil.getNextTime(sTime, 3);
	// sd = DateUtil.format(sTime, DateUtil.YYYYMMDD);
	// continue;
	// }
	// try {
	// Double avg = computeAvg(d, ilist, sTime, tag);
	// updateIndex2IndustryExtIndexTable(d, avg, tag, sTime, "avg");
	// } catch (Exception e) {
	// log.error("compute failed!", e);
	// }
	// try {
	// Double max = computeMax(d.getColumnName(), ilist);
	// updateIndex2IndustryExtIndexTable(d, max, tag, sTime, "max");
	// } catch (Exception e) {
	// log.error("compute failed!", e);
	// }
	// try {
	// Double min = computeMin(d.getColumnName(), ilist);
	// updateIndex2IndustryExtIndexTable(d, min, tag, sTime, "min");
	// } catch (Exception e) {
	// log.error("compute failed!", e);
	// }
	// // 取下一个时间点
	// sTime = StockUtil.getNextTime(sTime, 3);
	// sd = DateUtil.format(sTime, DateUtil.YYYYMMDD);
	// }
	// } catch (Exception e) {
	// // TODO: handle exception
	// log.error("update all data failed!", e);
	// }
	//
	// }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Double computeMax(final String columnName, List<Map> ilist) {
		if (ilist == null || ilist.size() == 0)
			return null;
		Double mrd = null;
		for (Map m : ilist) {
			Object to = m.get(columnName);
			if (to != null) {
				Double td = (Double) to;
				// 给要比较的值赋初值
				if (mrd == null)
					mrd = td;
				if (td > mrd && td != 0)
					mrd = td;
			}
		}

		return mrd;
	}

	@SuppressWarnings("rawtypes")
	private Double computeMin(String columnName, List<Map> ilist) {
		if (ilist == null || ilist.size() == 0)
			return null;
		Double minrd = null;
		for (Map m : ilist) {
			Object to = m.get(columnName);
			if (to != null) {
				Double td = (Double) to;
				// 给要比较的值赋初值
				if (minrd == null)
					minrd = td;
				if (td < minrd && td != 0)
					minrd = td;
			}
		}

		return minrd;
	}


	@SuppressWarnings("rawtypes")
	public List<DataItem> getMaxMinAvgMid(String mark, String type,
			String indexcode, Date sTime, Date eTime) {
		List<DataItem> dil = new ArrayList<DataItem>();
		String iIndexCode = StockUtil.getIndustryCode(type, indexcode);
		String tableName = SExt.getUExtTableName(mark,SExt.EXT_TABLE_TYPE_1);
		System.out.println("tag:"+mark+";tablename:"+tableName);
		List<Map> ml = getIndexListMapFromIndustryExt(mark, tableName, iIndexCode,
						sTime, eTime);
		if (ml != null && ml.size() > 0) {
			for (Map m : ml) {
				Date time = (Date) m.get("time");
				Double v = (Double) m.get("value");

				DataItem di = new DataItem(time, v);
				dil.add(di);
			}
		}
		else
		{
			if (StockConstants.ravgType.equals(type))
			{
				ml = realIndustryIndexData(mark, indexcode,
						sTime, eTime);
				
				if (ml != null && ml.size() > 0) {
					for (Map m : ml) {
						Object o = m.get("time");
						Date time = DateUtil.format2YYYYMMDDHHMMSS(o.toString());
						Double v = (Double) m.get("value");

						DataItem di = new DataItem(time, v);
						dil.add(di);
					}
				}
			}
			
		}

		return dil;
	}

	private List<Map> realIndustryIndexData(String tag, String indexcode, Date stime, Date etime) {
		List<Map> ml = new ArrayList<Map>();
		Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
		while (stime.compareTo(etime) <= 0) {
			try {
				Date actime = IndexService.getInstance().formatTime_Tag(stime, d, tag);
				if(actime!=null)
				{
					IndexMessage req = SMsgFactory.getUDCIndexMessage(tag);
					req.setTime(actime);
					//List<Company> cl = CompanyService.getInstance().getCompanyByTag(tag);
					//req.setVariable(StockConstants.V_K_tag_companylist, cl);
					req.setIndexCode(d.getIndexCode());
					req.setNeedAccessCompanyBaseIndexDb(false);
					req.setNeedRealComputeIndustryValue(true);
					req.setNeedAccessExtIndexDb(false);
					req.setIndustryIndexType(StockConstants.ravgType);
					Double v = IndustryService.getInstance().realComputeIndustryIndex(d, req);
					if(v!=null&&v!=0.0)
					{
						Map<String,Object> m = new HashMap<String,Object>();
						m.put("time", DateUtil.format2String(actime));
						m.put("value", v);
						ml.add(m);
					}
				}
				
			} catch (Exception e) {
				log.error("compute rule failed!", e);
			}
			stime = StockUtil.getNextTimeV3(stime,
					3,Calendar.MONTH);
		}
		return ml;
	}

	@SuppressWarnings("rawtypes")
	public Double getMaxMinAvgMidOneTimeFromDB(String mark, String type,
			String indexcode, Date time) {
		Double ret = null;
		String iIndexCode = StockUtil.getIndustryCode(type, indexcode);
		String tableName = SExt.getUExtTableName(mark,SExt.EXT_TABLE_TYPE_1);
		List<Map> ml = getIndexListMapFromIndustryExt(mark, tableName, iIndexCode,
						time, time);
		if (ml != null && ml.size() > 0) {
			Map m = ml.get(0);
			ret = (Double) m.get("value");
			return ret;
		}

		return ret;
	}

	public List<Map> getIndexListMapFromIndustryExt(String mark,String tableName, String iIndexCode, Date sTime, Date eTime) {
		Object rd = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("sTime", DateUtil.format2String(sTime));
		m.put("eTime", DateUtil.format2String(eTime));
		m.put("tableName", tableName);
		m.put("iIndexCode", iIndexCode);
		m.put("mark", mark);
		RequestMessage req = DAFFactory.buildRequest(
				"getIndexListMapFromIndustryExt", m, StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;
	}
	
	public List<Map> getIndustryIndexListOfIndexsMapFromDb(String mark,String indexlist, Date sTime, Date eTime,String type) {
		Object rd = null;
		
		String tableName = SExt.getUExtTableName(mark,SExt.EXT_TABLE_TYPE_1);
		Map<String, String> m = new HashMap<String, String>();
		m.put("stime", DateUtil.format2String(sTime));
		m.put("etime", DateUtil.format2String(eTime));
		m.put("tableName", tableName);
		m.put("indexlist", indexlist);
		m.put("mark", mark);
		RequestMessage req = DAFFactory.buildRequest(
				"getIndustryIndexListOfIndexsMapFromDb", m, StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;
	}
	
	public List<Map> getATagIndexListFromDb(String mark,String tableName, String iIndexCode, String sTime, String eTime) {
		Object rd = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("sTime", sTime);
		m.put("eTime", eTime);
		m.put("tableName", tableName);
		m.put("iIndexCode", iIndexCode);
		m.put("mark", mark);
		RequestMessage req = DAFFactory.buildRequest(
				"getIndexListMapFromIndustryExt", m, StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;
	}
	
	public List<Map> getOneIndexMMAM(String mark,String tableName, String indexcode, Date time) {
		Object rd = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", DateUtil.format2String(time));
		m.put("tableName", tableName);
		m.put("indexcode", indexcode);
		m.put("mark", mark);
		RequestMessage req = DAFFactory.buildRequest(
				"getOneIndexMMAM", m, StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;
	}
	/**
	 * 读取同一时间某一个行业指标的某类型数据
	 * 
	 * @param tableName
	 * @param time
	 * @param time
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public List<Map> getOneIndexAllMarkDataOneTimeByType(String type,
			String tableName, String indexcode, Date time) {
		Object rd = null;
		String iIndexCode = StockUtil.getIndustryCode(type, indexcode);
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", DateUtil.format2String(time));
		m.put("tableName", tableName);
		m.put("iIndexCode", iIndexCode);
		RequestMessage req = DAFFactory
				.buildRequest("getOneIndexAllMarkDataOneTimeByType", m,
						StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;

	}

	@SuppressWarnings("rawtypes")
	public List<Map> getAllMarkDataOneTimeByType(String type, String tableName,
			Date time) {
		Object rd = null;
		String prefix = StockUtil.getIndustryCodePrefix(type);
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", DateUtil.format2String(time));
		m.put("tableName", tableName);
		m.put("prefix", prefix);
		RequestMessage req = DAFFactory.buildRequest(
				"getAllMarkDataOneTimeByType", m, StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;

	}

	@SuppressWarnings("rawtypes")
	public List<Map> getOneIndexAllMarkData(String type, String tableName,
			String indexcode) {
		Object rd = null;
		String iIndexCode = StockUtil.getIndustryCode(type, indexcode);
		Map<String, String> m = new HashMap<String, String>();
		m.put("tableName", tableName);
		m.put("iIndexCode", iIndexCode);
		RequestMessage req = DAFFactory.buildRequest("getOneIndexAllMarkData",
				m, StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;

	}

	@SuppressWarnings("rawtypes")
	public Double getMaxMinAvgMidOneTimeFromCache(String mark, String type,
			String indexcode, Date time) {
		String iIndexCode = StockUtil.getIndustryCode(type, indexcode);
		String key = StockUtil.getExtCachekey(mark, iIndexCode, time);
		Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
		return IndustryExtCacheService.getInstance().get(key);
	}

	public Double getMaxMinAvgMidOneTimeWithCache(String mark, String type,
			String indexcode, Date time,boolean accesscache) {
		Double v = getMaxMinAvgMidOneTimeFromCache(mark, type, indexcode, time);
		if(accesscache&&v==null)
		{
			v = getMaxMinAvgMidOneTimeFromDB(mark, type, indexcode, time);
			if(v!=null)
			{
				String iIndexCode = StockUtil.getIndustryCode(type, indexcode);
				String key = StockUtil.getExtCachekey(mark, iIndexCode, time);
				Dictionary d = DictService.getInstance().getDataDictionary(indexcode);
				IndustryExtCacheService.getInstance().put(key, v);
			}
		}
		return v;
	}
	
	public String getIndustryIndexValueFromExt(String mark, String indexcode,
			int type, Date time) {
		String tableName = SExt.getUExtTableName(mark,SExt.EXT_TABLE_TYPE_1);
		Object rd = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("time", DateUtil.format2String(time));
		m.put("columnName", "value");
		m.put("tableName", tableName);
		m.put("mark", mark);
		m.put("iIndexCode",
				StockUtil.getIndustryCode(String.valueOf(type), indexcode));
		RequestMessage req = DAFFactory.buildRequest(
				"getIndustryIndexValueFromExt", m, StockConstants.common);
		rd = pLayerEnter.queryForObject(req);
		if (rd == null) {
			return null;
		}
		return String.valueOf(rd);
	}

	/**
	 * 取某一时间点，某一tag的，max,min,avg,mid
	 * 格式：max:min:avg:mid
	 * @param mark
	 * @param type
	 * @param indexcode
	 * @param time
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public String getMMAM(String tag, String indexcode, Date time) {
		StringBuilder sb = new StringBuilder();

		Double max = null;
		Double min = null;
		Double avg = null;
		Double mid = null;

		String tableName = SExt.getUExtTableName(tag,SExt.EXT_TABLE_TYPE_1);
		List<Map> ml = getOneIndexMMAM(tag,
				tableName, indexcode, time);
		if (ml != null && ml.size() > 0) {
			for (Map m : ml) {
				Double v = (Double) m.get("value");
				String iindexcode = (String) m.get("INDEX_CODE");
				if (iindexcode.contains("max"))
					max = v;
				if (iindexcode.contains("min"))
					min = v;
				if (iindexcode.contains("avg"))
					avg = v;
				if (iindexcode.contains("mid"))
					mid = v;
			}
		}

		sb.append(SMathUtil.getDoubleString_V2(max, 2) + ":");

		sb.append(SMathUtil.getDoubleString_V2(min, 2) + ":");

		sb.append(SMathUtil.getDoubleString_V2(avg, 2) + ":");

		sb.append(SMathUtil.getDoubleString_V2(mid, 2));

		return sb.toString();
	}

	public Double getIndustryExtIndexValueFromCache(Dictionary d, IndexMessage msg) {

		String iIndexCode = StockUtil.getIndustryCode(msg.getIndustryIndexType(), msg.getIndexCode());
		String key = StockUtil.getExtCachekey(msg.getUidentify(), iIndexCode,
				msg.getTime());
		Double v = IndustryExtCacheService.getInstance().get(key);
		if(v==null&& StockCenter.getInstance().isNeedAccessDcss(msg))
		{
			//数据已全量缓存，不从数据库取，只从分布式缓存取
			v = DcssIndustryExtIndexServiceClient.getInstance().get(key);
			if(v==null)
				return null;
		}
		return v;
	}

	public String getIndustryCodeByCompanycode(String companyCode) {
		String indcode = "";
		Industry ind = getIndustryByCompanycode(companyCode);
		if(ind!=null) indcode = ind.getIndustryCode();
		return indcode;
	}
	
	public Industry getIndustryByCompanycode(String companyCode) {
		String indn = CompanyService.getInstance().getIndustryNameOfCompany(
				companyCode);
		return getIndustryYFZXByName(indn);
	}

	/**
	 * 取同级的所有行业
	 * 
	 * @param tagcode
	 * @return
	 */
	public List<Industry> getPeerIndustry(String tagcode) {
		List<Industry> il = new ArrayList<Industry>();
		Industry ind = getIndustryCSRCByCode(tagcode);
		
		if (ind != null) {
			//取所有结点在同一个级的行业
//			il = getPeerIndustryByDepth(ind.getDepth());
			//取同一个父结点的行业
			Industry pind = getIndustryCSRCByCode(ind.getParentCode());
			if(pind!=null)
			{
				TreeNode tn = getIndustryCSRCTreeFromCache(0).get(pind.getIndustryCode());
				il = tn.getChildrenIndustry();
			}
			if(il!=null&&il.size()<2)
				il = getPeerIndustryByDepth(ind.getDepth());
		}
		return il;
	}

	/**
	 * 取同级的所有行业
	 * 
	 * @param tagcode
	 * @return
	 */
	public List<Industry> getPeerYfzxIndustry(String tagcode) {
		List<Industry> il = new ArrayList<Industry>();
		Industry ind = getIndustryYfzxByCode(tagcode);
		
		if (ind != null) {
			//取所有结点在同一个级的行业
//			il = getPeerIndustryByDepth(ind.getDepth());
			//取同一个父结点的行业
			Industry pind = getIndustryYfzxByCode(ind.getParentCode());
			if(pind!=null)
			{
				TreeNode tn = getIndustryYFZXTreeFromCache(0).get(pind.getIndustryCode());
				il = tn.getChildrenIndustry();
			}
			if(il!=null&&il.size()<2)
				il = getPeerIndustryByDepth(ind.getDepth());
		}
		return il;
	}
	
	private List<Industry> getPeerIndustryByDepth(int depth) {
		List<Industry>  retli = new ArrayList<Industry>();
		List<Industry> il = getIndustryCSRCTreeFromCache(0).get(root).getChildAndChild();
		for(Industry ind : il)
		{
			if(ind.getDepth()==depth)
			 retli.add(ind);
		}
		return retli;
	}

	public List<Industry> getPeerYfzxIndustryByDepth(int depth) {
		List<Industry>  retli = new ArrayList<Industry>();
		List<Industry> il = getIndustryYFZXTreeFromCache(0).get(root).getChildAndChild();
		for(Industry ind : il)
		{
			if(ind.getDepth()==depth)
			 retli.add(ind);
		}
		return retli;
	}
	public Double getIndustryExtIndexValue(Dictionary d, IndexMessage msg) {
		Double v = getIndustryExtIndexValueFromCache(d, msg);
		if(msg.isNeedAccessExtIndexDb()&&(v==null||v==0))
		{
			v = getIndustryExtIndexValueFromDb(d,msg);
			if(v!=null&&v!=0)
			{
				String iIndexCode = StockUtil.getIndustryCode(msg.getIndustryIndexType(), msg.getIndexCode());
				String key = StockUtil.getExtCachekey(msg.getUidentify(), iIndexCode,
						msg.getTime());
				IndustryExtCacheService.getInstance().put(key, v);
			}
		}
		return v;
	}

	private Double getIndustryExtIndexValueFromDb(Dictionary d, IndexMessage msg) {
		// TODO Auto-generated method stub
		return getMaxMinAvgMidOneTimeFromDB(msg.getUidentify(), msg.getIndustryIndexType(), msg.getIndexCode(), msg.getTime());
	}
	
	public Double getIndustryBaseIndexValue(IndexMessage req) {
		Dictionary d = DictService.getInstance().getDataDictionary(req.getIndexCode());
		
		Double v = getIndustryExtIndexValue(d, req);
		if(req.isNeedRealComputeIndustryValue()&&v==null&&req.getIndustryIndexType().equals(StockConstants.ravgType))
		{
			v = realComputeIndustryBaseIndex(d, req);
			if(v!=null)
			{
				String iIndexCode = StockUtil.getIndustryCode(req.getIndustryIndexType(), req.getIndexCode());
				String key = StockUtil.getExtCachekey(req.getUidentify(), iIndexCode,
						req.getTime());
				IndustryExtCacheService.getInstance().put(key, v);
			}
		}
		return v;
	}
	
	public  Double realComputeIndustryBaseIndex(Dictionary d, Message para) {
		IndexMessage im = (IndexMessage) para.clone();
		List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(im.getUidentify());
		Double sum = 0.0;// 行业的合值
		 if(cl==null||cl.size()==0)
		 {
			 //log.error("industry error cl.size is 0,tag ="+im.getTag());
			 return sum;
		 }
		for (Company c : cl) {
			if (CompanyService.getInstance().needRemoveWhenComputeIndustry(c))
				continue;
			if(c==null||c.getTsc()==null)
			{
				log.error("c or tsc is null"+c);
				continue;
			}
				
			if(!MatchinfoService.getInstance().tscIsMatch(d.getTableCode(), c.getTsc()))
				continue;
			im.setCompanyCode(c.getCompanyCode());
			im.setNeedAccessCompanyBaseIndexDb(false);
			Double cv = IndexService.getInstance().getCompanyBaseIndexValue(im);
			if (cv==null)
				continue;

			sum += cv;
		}
		return sum;
	}

	public List<Map> getATagAllBaseIndexListFromDb(String tag,
			String tableName, String prefix, Date stime, Date etime) {
		Object rd = null;
		Map<String, String> m = new HashMap<String, String>();
		m.put("sTime", DateUtil.format2String(stime));
		m.put("eTime", DateUtil.format2String(etime));
		m.put("tableName", tableName);
		m.put("mark", tag);
		m.put("prefix", prefix);
		RequestMessage req = DAFFactory.buildRequest(
				"getATagAllBaseIndexListFromDb", m, StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;
	}

	public void cacheIndustryMidResult(Dictionary d, IndexMessage midmsg,
			Double value) {
		String iIndexCode = StockUtil.getIndustryCode(midmsg.getIndustryIndexType(), midmsg.getIndexCode());
		String key = StockUtil.getExtCachekey(midmsg.getUidentify(), iIndexCode,
				midmsg.getTime());
		IndustryExtCacheService.getInstance().put(key,value);
	}

//	public List<Double> getCompanyIndexValueDoubleList(IndexMessage im) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	public List<Map> getIndustryIndexValueMapList(IndexMessage im) {
		String tableName = SExt.getUExtTableName(im.getUidentify(),SExt.EXT_TABLE_TYPE_1);
		return getIndexListMapFromIndustryExt(im.getUidentify(), tableName, im.getIndexCode(), im.getStartTime(), im.getEndTime());
	}

	public Industry getIndustryByCompany(Company c) {
		 Industry ind = IndustryService.getInstance().getIndustryCSRCByName(c.getIndustry());
		 if(ind==null)
			 ind = IndustryService.getInstance().getIndustryCSRCByName(c.getF041v());
		 if(ind==null)
			 ind = IndustryService.getInstance().getIndustryCSRCByName(c.getF040v());
		 if(ind==null)
			 ind = IndustryService.getInstance().getIndustryCSRCByName(c.getF039v());
		return ind;
	}

	public Stock getIndustryDashBoardByTag(String tag,Date time) {
		Industry ind = getIndustryCSRCByName(tag);
		Stock s = new Stock();
		s.setStockcode(ind.getIndustryCode());// 000001.sz 需要.sz
		s.setSimplename(ind.getName());

		// 取净资产收益率
		String roe = getIndustryIndexValueFromExt(tag, "2022", Integer.valueOf(StockConstants.ravgType), time);
		if (roe != null)
			s.setRoe(SMathUtil.getDouble(Double.valueOf(roe), 2));

		// 取pb值
		IndexMessage im = SMsgFactory.getUMsg(tag);
		im.setTime(time);
		im.setIndexCode("2350");// 市净率
		Double pb = IndexValueAgent.getIndexValue(im);
		pb = SMathUtil.getDouble(pb, 2);
		s.setPb(pb);

		// 取pe值
		IndexMessage im1 = SMsgFactory.getUMsg(tag);
		im1.setTime(time);
		im1.setIndexCode("2349");// 市盈率（经调整）
		Double pe = IndexValueAgent.getIndexValue(im1);
		pe = SMathUtil.getDouble(pe, 2);
		s.setPe(pe);

		// 取ps值
		IndexMessage im2 = SMsgFactory.getUMsg(tag);
		im2.setTime(time);
		im2.setIndexCode("2352");// 市销率
		Double ps = IndexValueAgent.getIndexValue(im2);
		ps = SMathUtil.getDouble(ps, 2);
		s.setPs(ps);

		return s;
	}


	


	public List<Map> getIndustryBaseIndexDataListMapFromDb(String tableName,Date stime,
			Date etime, String type) {
		Object rd = null;
		Map<String, String> m = new HashMap<String, String>();
		String prefix = StockUtil.getIndustryCodePrefix(type);
		m.put("stime", DateUtil.format2String(stime));
		m.put("etime", DateUtil.format2String(etime));
		m.put("tableName", tableName);
		m.put("prefix", prefix);
		RequestMessage req = DAFFactory.buildRequest(
				"getIndustryBaseIndexDataListMapFromDb", m, StockConstants.common);
		rd = pLayerEnter.queryForList(req);
		if (rd == null) {
			return null;
		}
		return (List<Map>) rd;
	}

	public String getZJSCacheKey(String key) {
		// TODO Auto-generated method stub
		return "zjs_"+key;
	}
	
	public String getYFZXCacheKey(String key) {
		// TODO Auto-generated method stub
		return "yfzx_"+key;
	}

	/**
	 * 只对ravg类型
	 * @param iim
	 * @return
	 */
	public Double computeIndustryIndex(IndexMessage iim) {
		Double v = StockConstants.DEFAULT_DOUBLE_VALUE;
		Dictionary d = DictService.getInstance().getDataDictionary(iim.getIndexCode());
		if(d==null) return v;
		if(!iim.getIndustryIndexType().equals(StockConstants.ravgType))
		{
			log.error("industry type error!");
			return v;
		}
		v = getIndustryExtIndexValue(d, iim);
		if((v==null||v==0)&&iim.isNeedRealComputeIndustryValue())
		{
			v = realComputeIndustryIndex(d,iim);
			if(v!=null&&v!=0)
			{
				String iIndexCode = StockUtil.getIndustryCode(iim.getIndustryIndexType(), iim.getIndexCode());
				String key = StockUtil.getExtCachekey(iim.getUidentify(), iIndexCode,
						iim.getTime());
				IndustryExtCacheService.getInstance().put(key, v);
			}
		}
		return v;
	}

	public Double realComputeIndustryIndex(Dictionary d, IndexMessage iim) {
		Double v = null;
		if(StockUtil.isBaseIndex(d.getType()))
		{
			v = realComputeIndustryBaseIndex(d, iim);
		}
		else
		{
			v = realComputeIndustryExtIndex(d,iim);
		}
		return v;
	}

	private Double realComputeIndustryExtIndex(Dictionary d, IndexMessage iim) {
		Cfirule crule = CRuleService.getInstance().getCfruleByCodeFromCache(d.getIndexCode());
		if(crule==null)
		{
//			log.error("rule is null;d:"+d);
			return StockConstants.DEFAULT_DOUBLE_VALUE;
		}
		return CRuleService.getInstance().computeIndex(iim, crule);
	}

	public List<String> getAllMainTags() {
		List<String> ams = new ArrayList<String>();
		List<String> ls = CompanyService.getInstance().getAllTags();
		for(String t : ls)
		{
			if(t.startsWith("mtag_"))
			{
				ams.add(t);
			}
		}
		return ams;
	}

	public String getIndOneChartIndexData(String companycode, String xindexcode,
			String yindexcode, Date stime, Date etime) {
		String ret = "";
		if(!StringUtil.isEmpty(xindexcode))
		{
			Cfirule cfrule = CRuleService.getInstance().getCfruleByCodeFromCache(xindexcode);
			if(cfrule!=null)
			{
				String rule = cfrule.getRule();
				if(rule.matches(StockRegex.PATTERN_FUNCTION))
				{
					Map<String,String> nrm = GetIndustryValue.getIntance().getIndexcodeFromRuleAndTag(rule, companycode);
					String nindexcode = nrm.get("indexcode");
					String ntag = nrm.get("tag");
					if(!StringUtil.isEmpty(nindexcode)&&!StringUtil.isEmpty(ntag))
					{
						ret = DcssIndustryExtIndexServiceClient.getInstance()
								.getOneChartIndIndexData(ntag, nindexcode, "", stime,
										etime, StockConstants.ravgType);
						if(!StringUtil.isEmpty(ret))
							ret=ntag+"&"+ret;
					}
				}
			}
		}
		
		if(!StringUtil.isEmpty(yindexcode))
		{
			Cfirule cfrule = CRuleService.getInstance().getCfruleByCodeFromCache(yindexcode);
			if(cfrule!=null)
			{
				String rule = cfrule.getRule();
				if(rule.matches(StockRegex.PATTERN_FUNCTION))
				{
					Map<String,String> nrm = GetIndustryValue.getIntance().getIndexcodeFromRuleAndTag(rule, companycode);
					String nindexcode = nrm.get("indexcode");
					String ntag = nrm.get("tag");
					if(!StringUtil.isEmpty(nindexcode)&&!StringUtil.isEmpty(ntag))
					{
						ret = DcssIndustryExtIndexServiceClient.getInstance()
								.getOneChartIndIndexData(ntag, "", nindexcode, stime,
										etime, StockConstants.ravgType);
						if(!StringUtil.isEmpty(ret))
							ret=ntag+"&"+ret;
					}
				}
			}
		}
		return ret;
	}


	public Industry getYfzxIndustryByName(String name,int depth)
	{
		Industry ind = null;
		TreeNode tn = getIndTreeNodeByDepth(name, depth);
		if(tn!=null&&tn.getReference()!=null)
			ind = (Industry) tn.getReference();
		return ind;
	}
	/**
	 * 单级查
	 * @param name
	 * @param pid
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Industry getYfzxIndustryFromParent(String name,String pid)
	{
		TreeNode tn = IndustryService.getInstance().getIndustryYFZXTreeFromCache().get(pid);
		if(tn!=null)
		{
			List<TreeNode> ctnl = tn.getChildren();
			if(ctnl!=null)
			{
				for(TreeNode ctn:ctnl)
				{
					Object o = ctn.getReference();
					if(o!=null)
					{
						Industry ind =(Industry) o;
						if(ind.getName().split("_")[2].equals(name))
							return ind;
					}
				}
			}
		}
		return null;
	}
	
	private TreeNode getIndTreeNodeByDepth(String name, int depth) {
		HashMap<String, TreeNode> rm = IndustryService.getInstance().getIndustryYFZXTreeFromCache(1);
		if(rm!=null)
		{
			Iterator<String> iter = rm.keySet().iterator();
			while(iter.hasNext())
			{
				String k = iter.next();
				TreeNode tn = rm.get(k);
				Industry ind = (Industry) tn.getReference();
				String tname = k.split("_")[2].trim();
				if(tname.equals(name))
				{
					if(ind.getDepth()==depth)
						return tn;					
				}
			}
		}
		return null;
	}
	
	public void initIndustryJson()
	{
		try {
			SmallIndustry si = initIndustryJson(IndustryService.root);
			LCEnter.getInstance().put(SCache.YFZX_ALL_INDUSTRY_JSON,
					JSONUtil.serialize(si), SCache.CACHE_NAME_INDUSTRY);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private SmallIndustry initIndustryJson(String pid) {
		SmallIndustry si = new SmallIndustry();
		Industry ind = IndustryService.getInstance().getIndustryYfzxByCode(pid);
		if(ind==null) return null;
		String name = ind.getName()+":"+ind.getIndustryCode();
		si.setN(name);
		List<Industry> l = IndustryService.getInstance().getIndustryYFZXTree().get(pid).getChildrenIndustry();
		for(int i=0;i<l.size();i++)
		{
			//分隔指标编码与名字
			Industry tind = l.get(i);
			String tindustryCode = tind.getIndustryCode();
			if(tindustryCode==null)
				continue;
			//去掉下面没有公司的分类
			List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(tind.getName().toLowerCase());
			if(cl==null||cl.size()==0) 
				continue;
			SmallIndustry csi =initIndustryJson(tindustryCode);
			si.addChild(csi);
		}
		return si;
		
	}

	/**
	 * 取板块中最早上市的公司的上市时间
	 * @param uidentify
	 * @return
	 */
	public Date getPlatePulishTime(String uidentify) {
		Date d = new Date();
		List<Company> cl = CompanyService.getInstance().getCompanyListByTagFromCache(uidentify);
		if(cl!=null)
		{
			for(Company c :cl)
			{
				Date cd = CompanyService.getInstance().getCompanyPulishTime(c.getCompanyCode());
					if(cd.compareTo(d)<0)
						d = cd;
			}
		}
		return d;
	}
	
	public Boolean haveIndustryData(String companyCode) {
		Industry ind = getIndustryByCompanycode(companyCode);
		if(ind==null) return false;
		Date time = StockUtil.getCurIndustryJiDuTime();
		IndexMessage im1 = SMsgFactory.getUMsg(ind.getName(), "2022",time );
		im1.setNeedAccessExtIndexDb(false);
		im1.setNeedAccessCompanyBaseIndexDb(false);
		Double xv = IndexValueAgent.getIndexValue(im1);
		if(xv!=null&&xv!=0)
			return true;
		return false;
	}
	
}
