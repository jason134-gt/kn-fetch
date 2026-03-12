package com.yz.stock.portal.action;

import org.apache.commons.configuration.Configuration;
import org.apache.struts2.convention.annotation.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.util.StringUtil;
import com.yz.mycore.core.manager.BaseFactory;
import com.yz.stock.common.BaseAction;
import com.yz.stock.portal.excel.AcessImportDataExcelService;
import com.yz.stock.portal.excel.ImportAddDataExcelService;
import com.yz.stock.portal.excel.ExcelService;
import com.yz.stock.portal.excel.Export2ExcelService;
import com.yz.stock.portal.excel.StockDataExcelService;
import com.yz.stock.portal.manager.ComputeIndexManager;

public class exportDataAction extends BaseAction {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5424611799257286358L;
	Logger log = LoggerFactory.getLogger(this.getClass());
	
	

		@Action(value = "/file/exportData2File")
		public String exportData2File()
			{
				try {
					String companyCode = this.getHttpServletRequest().getParameter("exportCompanyCode");
					String type = this.getHttpServletRequest().getParameter("type");
					if(StringUtil.isEmpty(companyCode)||StringUtil.isEmpty(type))
						return ERROR;
					Configuration c = BaseFactory.getConfiguration();
					String exportDir = c.getString("stock.exportDir");
					Export2ExcelService.getInstance().exportData2File(exportDir,companyCode.trim(),Integer.valueOf(type));
				} catch (Exception e) {
					// TODO: handle exception
					log.error("impport data failed!",e);
					return ERROR;
				}
				return SUCCESS;
			}
		
		/**
		 * 唐斌奇 增加2012-07-18 导出行业Excel,里面带行业里的公司
		 * @return
		 */
		@Action(value = "/file/export/xls/industry")
		public String exportData2FileOnIndustry(){
			Configuration c = BaseFactory.getConfiguration();
			String exportDir = c.getString("stock.exportDir");
			Export2ExcelService.getInstance().exportData2FileOnIndustry(exportDir);
			return SUCCESS;
		}
}
