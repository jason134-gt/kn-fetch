package com.yz.stock.portal.service;

import java.util.List;

import com.yz.mycore.core.manager.BaseFactory;
import com.yz.mycore.daf.bean.RequestMessage;
import com.yz.mycore.daf.enter.PLayerEnter;
import com.yz.mycore.daf.manager.DAFFactory;
import com.stock.common.constants.StockConstants;
import com.yz.stock.portal.model.CompanyStockInfo;

public class CompanyStockInfoDataService {

	private static CompanyStockInfoDataService instance = new CompanyStockInfoDataService();
	PLayerEnter pLayerEnter = BaseFactory.getPLayerEnter();

	private CompanyStockInfoDataService() {

	}

	public static CompanyStockInfoDataService getInstance() {
		return instance;
	}

	public String insert(CompanyStockInfo csinfo) {
		CompanyStockInfo csi = queryByCompanyStockCodeA2H(csinfo.getObSecid0007());
		if (csi != null) {
			return update(csinfo);
		}
		String sqlMapKey = "com.yz.stock.portal.model.CompanyStockInfo.insert";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, csinfo,
				StockConstants.common);
		return pLayerEnter.insert(req);

	}

	private String update(CompanyStockInfo csinfo) {
		// TODO Auto-generated method stub
		String sqlMapKey = "com.yz.stock.portal.model.CompanyStockInfo.update";
		RequestMessage req = DAFFactory.buildRequest(sqlMapKey, csinfo,
				StockConstants.common);
		return pLayerEnter.modify(req);
	}

	// 通过机构id查询
	public List<CompanyStockInfo> queryBySecid(CompanyStockInfo csinfo) {
		String sqlMapKey = "com.yz.stock.portal.model.CompanyStockInfo.getObSecid0007";
		RequestMessage req = DAFFactory.buildRequest(csinfo.getObSecid0007(),
				sqlMapKey, csinfo, StockConstants.companyStockInfo);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<CompanyStockInfo>) o;
	}

	// 通过证券代码查询
	public List<CompanyStockInfo> queryByCompanyStockCode(String companyStockCode) {
		CompanyStockInfo csi = new CompanyStockInfo();
		csi.setObSeccode0007(companyStockCode.trim());
		String sqlMapKey = "com.yz.stock.portal.model.CompanyStockInfo.getObSeccode0007";
		RequestMessage req = DAFFactory.buildRequest(
				sqlMapKey, csi, StockConstants.companyStockInfo);
		Object o = pLayerEnter.queryForList(req);
		if (o == null)
			return null;
		return (List<CompanyStockInfo>) o;

	}
	
	
	// 通过证券代码查询,只查属于A--H证券类型的公司
		public CompanyStockInfo queryByCompanyStockCodeA2H(String companyStockCode) {
			CompanyStockInfo csi = new CompanyStockInfo();
			csi.setObSeccode0007(companyStockCode.trim());
			String sqlMapKey = "com.yz.stock.portal.model.CompanyStockInfo.getObSeccode0007A2H";
			RequestMessage req = DAFFactory.buildRequest(csi.getObSeccode0007(),
					sqlMapKey, csi, StockConstants.companyStockInfo);
			Object o = pLayerEnter.queryForObject(req);
			if (o == null)
				return null;
			return (CompanyStockInfo) o;

		}
		
		// 查询指定证券类型的证券信息
		public CompanyStockInfo queryByCompanyCodeAndType(String companyCode,
				String stockTypeCode) {
			CompanyStockInfo csi = new CompanyStockInfo();
			csi.setObSecid0007(companyCode.trim());
			csi.setF002v0007(stockTypeCode);
			String sqlMapKey = "com.yz.stock.portal.model.CompanyStockInfo.getobSecid0007ByStockType";
			RequestMessage req = DAFFactory.buildRequest(csi.getObSeccode0007(),
					sqlMapKey, csi, StockConstants.companyStockInfo);
			Object o = pLayerEnter.queryForObject(req);
			if (o == null)
				return null;
			return (CompanyStockInfo) o;

		}
		
		// 通过证券代码查询
		public List<CompanyStockInfo> queryByCompany2Code(String companyCode) {
			CompanyStockInfo csi = new CompanyStockInfo();
			csi.setObSeccode0007(companyCode.trim());
			String sqlMapKey = "com.yz.stock.portal.model.CompanyStockInfo.getobSecid0007A2H";
			RequestMessage req = DAFFactory.buildRequest(
					sqlMapKey, csi, StockConstants.companyStockInfo);
			Object o = pLayerEnter.queryForList(req);
			if (o == null)
				return null;
			return (List<CompanyStockInfo>) o;

		}
		
		
}
