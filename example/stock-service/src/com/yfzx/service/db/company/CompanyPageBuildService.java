package com.yfzx.service.db.company;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import com.stock.common.model.Company;
import com.stock.common.model.company.Company0010;
import com.stock.common.model.company.Company0012;
import com.stock.common.model.company.Company0013;
import com.stock.common.model.company.Company0018;
import com.stock.common.model.company.Company0019;
import com.stock.common.model.company.Company0021;
import com.stock.common.model.company.Index0001;
import com.stock.common.model.company.Stock0010;
import com.stock.common.model.company.Stock0011;
import com.stock.common.model.company.Stock0013;
import com.yfzx.dc.template.VmUtil;
import com.yfzx.service.db.CompanyService;


/**
 * 公司主页构造服务
 * @author wind
 *
 */
public class CompanyPageBuildService {
	
	
	private final static String COMPANY_PATH="company";
	private final static String INDEX_VM="index.html",DSH_VM="td_dsh.html",JSH_VM="td_jsh.html",JLTD_VM="td_gg.html",INFO_VM="info.html",
			GBJG_VM="gb_jg.html",GBGD_VM="gb_gd.html",SU_VM="su.html";	
	private static CompanyPageBuildService cpbs = new CompanyPageBuildService();	
	CompanyService cs = CompanyService.getInstance();
	Company0021Service c0021s = Company0021Service.getInstance();
	Company0010Service c0010s = Company0010Service.getInstance();
	
	private CompanyPageBuildService(){		
	}
	
	public static CompanyPageBuildService getInstance(){
		return cpbs;
	}
	
	public void buildAllHtml(String realPath,String contextPath){
		
		List<Company> companyArr =  cs.getCompanyList();		
		int i =0;
		for(Company company:companyArr){				
			if(!"013007".equals(company.getF031v())){//不等于预披露的公司，包括上市和ST等
				if(i++>3)break;
				String saveDirStr = realPath + File.separator + "html" + File.separator +COMPANY_PATH+File.separator + company.getStockCode() ;
				buildIndexHtml(saveDirStr,company,contextPath);
				buildInfoHtml(saveDirStr,company,contextPath);
				buildTDDSHHtml(saveDirStr,company,contextPath);
				buildTDJSHHtml(saveDirStr,company,contextPath);
				buildTDJLTDHtml(saveDirStr,company,contextPath);
				buildGBJGHtml(saveDirStr,company,contextPath);
				buildGDHtml(saveDirStr,company,contextPath);
				buildSUHtml(saveDirStr,company,contextPath);
				buildFXHtml(saveDirStr,company,contextPath);
			}
		}
	}
	
	private void buildIndexHtml(String saveDirStr,Company company,String contextPath){		
		buildHtmlPage(saveDirStr,INDEX_VM,null);
	}
	
	/**
	 * 生成页面:公司简介信息
	 * @param saveDirStr
	 * @param company
	 * @param contextPath
	 */
	@SuppressWarnings("unchecked")
	private void buildInfoHtml(String saveDirStr,Company company,String contextPath){
		try {
			Map<String,Object> map = new HashMap<String,Object>(); //= BeanUtils.describe(company);
			map.put("company", company);
			map.put("dateformat", new SimpleDateFormat("yyyy-MM-dd"));
			map.put("contextPath", contextPath);			
			buildHtmlPage(saveDirStr,INFO_VM,map);			
		} catch (Exception e) {			
			e.printStackTrace();
		} 
				
	}
	
	/**
	 * 生成页面:公司团队之董事会
	 * @param saveDirStr
	 * @param company
	 * @param contextPath
	 */
	private void buildTDDSHHtml(String saveDirStr,Company company,String contextPath){
		Map<String,Object> map = new HashMap<String,Object>();
		List<Company0021> list = c0021s.getNowDSHList(company.getStockCode());
		map.put("list", list);
		map.put("contextPath", contextPath);	
		buildHtmlPage(saveDirStr,DSH_VM,map);
		
	}
	
	/**
	 * 生成页面:公司团队之监事会
	 * @param saveDirStr
	 * @param company
	 * @param contextPath
	 */
	private void buildTDJSHHtml(String saveDirStr,Company company,String contextPath){
		Map<String,Object> map = new HashMap<String,Object>();
		List<Company0021> list = c0021s.getNowJSHList(company.getStockCode());
		map.put("list", list);
		map.put("contextPath", contextPath);	
		buildHtmlPage(saveDirStr,JSH_VM,map);
		
	}
	
	/**
	 * 生成页面:公司团队之高管
	 * @param saveDirStr
	 * @param company
	 * @param contextPath
	 */
	private void buildTDJLTDHtml(String saveDirStr,Company company,String contextPath){
		Map<String,Object> map = new HashMap<String,Object>();
		List<Company0021> list = c0021s.getNowJLTDList(company.getStockCode());
		map.put("list", list);
		map.put("contextPath", contextPath);	
		buildHtmlPage(saveDirStr,JLTD_VM,map);
		
	}
	
	/**
	 * 生成页面:公司股本结构
	 * @param saveDirStr
	 * @param company
	 * @param contextPath
	 */
	private void buildGBJGHtml(String saveDirStr,Company company,String contextPath){
		List<Company0010> list = c0010s.getTop4Company0010(company.getStockCode());		
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("contextPath", contextPath);	
		map.put("dateformat", new SimpleDateFormat("yyyy-MM-dd"));
		if(list !=null){
			for(int i=0;i<list.size();i++){
				Company0010 c0010 = list.get(i);
				map.put("company0010V"+i, c0010);
			}
		}
		buildHtmlPage(saveDirStr,GBJG_VM,map);
	}
	
	/**
	 * 生成页面:公司股东
	 * @param saveDirStr
	 * @param company
	 * @param contextPath
	 */
	private void buildGDHtml(String saveDirStr,Company company,String contextPath){
		List<Company0012> company0012Array = Company0012Service.getInstance().getCompany0012Array(company.getStockCode());
		List<Company0013> company0013Array =Company0013Service.getInstance().getCompany0013Array(company.getStockCode());
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("vmUtil", new VmUtil());
		map.put("contextPath", contextPath);	
		map.put("company0012Array", company0012Array);
		map.put("company0013Array", company0013Array);
		buildHtmlPage(saveDirStr,GBGD_VM,map);
	}
	
	private void buildSUHtml(String saveDirStr,Company company,String contextPath){
		List<Company0018> company0018Array = Company0018Service.getInstance().getLastCompany0018Array(company.getStockCode());
		List<Company0019> company0019Array = Company0019Service.getInstance().getLastCompany0019Array(company.getStockCode());
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("vmUtil", new VmUtil());
		map.put("company0019Array", company0019Array);
		map.put("company0018Array", company0018Array);
		buildHtmlPage(saveDirStr,SU_VM,map);
	}
	
	private void buildFXHtml(String saveDirStr,Company company,String contextPath){
		Stock0010 stock0010 = Stock0010Service.getInstance().getSelect(company.getStockCode());
		List<Stock0011> stock0011Array = Stock0011Service.getInstance().getSelectList(company.getStockCode());
		List<Stock0013> stock0013Array = Stock0013Service.getInstance().getSelectList(company.getStockCode());
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("vmUtil", new VmUtil());
		map.put("stock0010", stock0010);
		map.put("stock0011Array",stock0011Array);
		map.put("stock0013Array", stock0013Array);
		buildHtmlPage(saveDirStr,"fx.html",map);		
	}
	
	private void buildIndexHtml(){
		Index0001 index0001 = new Index0001();
		index0001.setF001v("");
	}

	/**
	 * 构建HTML格式的页面
	 * @param saveDirStr 生成文件保存目录
	 * @param vmFile 模版文件名，又生成文件名
	 * @param map 模版里的变量Map
	 */
	private void buildHtmlPage(String saveDirStr,String vmFile,Map<String,Object> map) {
		File saveDir = new File(saveDirStr);
		try {
			if (!saveDir.exists()){
				saveDir.mkdirs();
			}
			VelocityContext context = new VelocityContext();
			if(map!=null){
				Iterator<Map.Entry<String,Object>> iter = map.entrySet().iterator(); 
				while (iter.hasNext()) { 
				    Map.Entry<String,Object> entry = iter.next(); 
				    context.put(entry.getKey(),entry.getValue()); 
				    
				} 
			}
			// 模板保存在WebRoot/web-inf/product目录下
			Template template = Velocity.getTemplate(COMPANY_PATH+File.separator+vmFile);
			FileOutputStream outStream = new FileOutputStream(new File(saveDir,
					vmFile));
			OutputStreamWriter writer = new OutputStreamWriter(outStream,
					"UTF-8");
			BufferedWriter sw = new BufferedWriter(writer);
			template.merge(context, sw);
			sw.flush();
			sw.close();
			outStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String priviewVM(String vmContent,Company vmCompany){
		List<Company0018> company0018Array = Company0018Service.getInstance().getLastCompany0018Array(vmCompany.getStockCode());
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("company", vmCompany);
		map.put("company0018Array", company0018Array);
		map.put("vmUtil", new VmUtil());
		VelocityContext context = new VelocityContext();
		if(map!=null){
			Iterator<Map.Entry<String,Object>> iter = map.entrySet().iterator(); 
			while (iter.hasNext()) { 
			    Map.Entry<String,Object> entry = iter.next(); 
			    context.put(entry.getKey(),entry.getValue()); 
			} 
		}		
		
		StringWriter outWriter = new StringWriter();
		try{
//			vmContent = formatVM(vmContent);
			boolean evaluateResult = Velocity.evaluate(context, outWriter, "CompanyVM", vmContent);
			if(!evaluateResult){
				System.out.println("VM hava error");
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return outWriter.toString();		
	}

	//不实现负责
//	private String formatVM(String vmContent){
//		NodeFilter companyVMNodeFilter = //new HasAttributeFilter("vm");
//				new NodeFilter() {	
//					private static final long serialVersionUID = 1014260271053623538L;		
//					// 实现该方法,用以过滤标签
//					@Override
//					public boolean accept(Node node) {
//						return true; 
//					}
//				};
//		NodeList companyVMNodeList = DownloadPage.parseHtml(vmContent, "utf-8", companyVMNodeFilter);
//		for (int i = 0; i < companyVMNodeList.size(); i++) {
//			Node node = companyVMNodeList.elementAt(i);   
//            //如果为链接节点   
//			if (node instanceof Tag){
//				Tag tag = (Tag)node;
//				String attrValue = tag.getAttribute("vm");
//				if(attrValue != null){
//					tag.getStartPosition();
//					tag.getEndPosition();
//					
//					tag.accept(new NodeVisitor() { 						 
//				        public void visitTag(Tag tag) { 
//				        	String top = tag.getAttribute("vm");
//				        	tag.removeAttribute("vm");				           
//				        } 
//				    });
//				}
//			}
//		}
//		return companyVMNodeList.toHtml();
//	}	
}
