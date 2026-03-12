/**
 * 
 */
package com.yz.stock.portal.service.company.spider;

import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.company.CompanyExt;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.company.CompanyExtService;


/**
 * @author wind
 * 未来考虑使用jsoup处理。Java上的jQuery?解析HTML利器———jsoup
 */
public class SpiderFacade implements Runnable {

	private static CompanyExtService ces = CompanyExtService.getInstance();
	static Logger logger = LoggerFactory.getLogger(SpiderFacade.class);
	
	public String getWindCompanyInfo(String stockCode){
		StringBuffer windCompanyInfo = new StringBuffer();
		String windCompanyUrl = "http://www.windin.com/home/stock/stock-co/"+stockCode+".shtml";
		String windCharset = "utf-8";
		String content = DownloadPage.getContentFormUrl(windCompanyUrl, windCharset);
		NodeFilter windCompanyNodeFilter =
			new NodeFilter() {

				private static final long serialVersionUID = 1014260271053623538L;		
				// 实现该方法,用以过滤标签
				public boolean accept(Node node) {							
					if(node instanceof TableRow){
						String classname=((TableRow)node).getAttribute("class");
						if("greybg".equals(classname)){
							return true;
						}
					}							
					return false;
				}
			};
		NodeList windCompanyNodeList = DownloadPage.parseHtml(content, windCharset, windCompanyNodeFilter);
		
		for (int i = 0; i < windCompanyNodeList.size(); i++) {   
		TableRow n = (TableRow) windCompanyNodeList.elementAt(i);
	        TableColumn[] tds = n.getColumns();
	        int index =-1;
	        for(int j=0;j<tds.length;j++){
	        	TableColumn td = tds[j];
	        	if("公司简介".equals(td.getStringText())){
	        		index = j+1;
	        		break;
	        	}
	        }
        if(index!=-1)windCompanyInfo.append(tds[index].getStringText());
		} 
		return windCompanyInfo.toString();
	}
	
	public String getWindCompanyProduct(String stockCode){
		StringBuffer windCompanyProduct = new StringBuffer();
		String windCompanyProductUrl = "http://www.windin.com/home/stock/stock-pr/"+stockCode+".shtml";
		String windCharset = "utf-8";
		String content = DownloadPage.getContentFormUrl(windCompanyProductUrl, windCharset);
		
		int startInt =content.toUpperCase().indexOf("<pt:Stock-MA/>".toUpperCase())+"<pt:Stock-MA/>".length();
		content = content.substring(startInt);
		int	endInt = content.toUpperCase().indexOf("</table>".toUpperCase())+"</table>".length();
		content = content.substring(0,endInt);

		NodeFilter windCompanyProductNodeFilter =
			new NodeFilter() {	
				private static final long serialVersionUID = 1014260271053623538L;		
				// 实现该方法,用以过滤标签

				public boolean accept(Node node) {
					if(node instanceof TableColumn){//抓取此Table中的TD
						return true;
					}
					return false;
				}
			};
		NodeList windCompanyProductNodeList = DownloadPage.parseHtml(content, windCharset, windCompanyProductNodeFilter);
		for (int i = 0; i < windCompanyProductNodeList.size(); i++) { 
			TableColumn td = (TableColumn) windCompanyProductNodeList.elementAt(i);	  
			Node[] td2node = td.getChildrenAsNodeArray();
			if(td2node.length > 1){						
				for(int j=0;j< td2node.length;j++){
		        	 if(td2node[j] instanceof LinkTag){
		        		LinkTag linkNode = (LinkTag)td2node[j];
		        		windCompanyProduct.append(linkNode.getStringText()).append(";");
		        	 }
	        	}
			}else{
				if(td2node[0] instanceof TextNode){
					windCompanyProduct.append(td2node[0].getText()).append(";");	
				}				
			}
		} 
		return windCompanyProduct.toString();	
	}
	
	public void saveDB(String stockCode){		
		String str1 = getWindCompanyInfo(stockCode);
		//需要能抓取万点的公司简介
		if(!StringUtil.isEmpty(str1.trim())){
			String str2 = getWindCompanyProduct(stockCode);
			CompanyExt companyExt = new CompanyExt();		
			companyExt.setStockCode(stockCode);		
			companyExt.setCompanyInfo(str1);
			companyExt.setCompanyProduct(str2);
			ces.saveDB(companyExt);
		}		
	}

	public static void main(String[] args) {
		SpiderFacade sf = new SpiderFacade();
	
		sf.run();
		//System.out.println(sf.getWindCompanyProduct("000002.sz"));
	}
	

	public void run() {	
		
		//List<Company> companyArr =  CompanyService.getInstance().getCompanyList();
//		int i =0;
		//for(Company company:companyArr){
//		if(!"013007".equals(company.getF031v())){//不等于预披露的公司，包括上市和ST等
////			if(i++>3)break;
//			try{
//				saveDB(company.getStockCode());
//			}catch(Exception e){
//				logger.error("抓取异常:"+e);
//			}
//		}
		if(StringUtil.isEmpty(companyCodeArr) == false){
			String cs =  companyCodeArr ;//"300367.sz";
			String[] cArr = cs.split(",");
			for(String companycode :cArr){
				companycode = companycode.toUpperCase();
				try{
					saveDB(companycode);
				}catch(Exception e){
					logger.error("抓取异常:"+e);
				}
			}
		}
	}
	
	public String getCompanyCodeArr() {
		return companyCodeArr;
	}

	public void setCompanyCodeArr(String companyCodeArr) {
		this.companyCodeArr = companyCodeArr;
	}

	//增加外部获取要增加公司列表
	private String companyCodeArr;
}
