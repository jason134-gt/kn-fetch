/**
 * 
 */
package com.yz.stock.portal.service.company.spider;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.PrototypicalNodeFactory;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.tags.CompositeTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

public class DownloadPage {

	/**
	 * 根据URL抓取网页内容
	 * 
	 * @param url
	 * @return
	 */
	public static String getContentFormUrl(String url,String charset) {
		/* 实例化一个HttpClient客户端 */
		HttpClient client = new DefaultHttpClient();
		HttpGet getHttp = new HttpGet(url);
		getHttp.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 5.1) "
				+ "AppleWebKit/535.1 (KHTML, like Gecko) "
				+ "Chrome/14.0.802.30 Safari/535.1 SE 2.X MetaSr 1.0");
		getHttp.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		getHttp.setHeader("Cookie", "pgv_pvi=3233368064; pgv_si=s5717884928");

		String content = null;
		HttpResponse response;
		try {
			/* 获得信息载体 */
			response = client.execute(getHttp);

			HttpEntity entity = response.getEntity();
			VisitedUrlQueue.addElem(url);
			if (entity != null) {
				/* 转化为文本信息 */
				content = EntityUtils.toString(entity, charset);
				/* 判断是否符合下载网页源代码到本地的条件 */
//				if (FunctionUtils.isCreateFile(url)
//						&& FunctionUtils.isHasGoalContent(content) != -1) {
//					FunctionUtils.createFile(
//							FunctionUtils.getGoalContent(content), url);
//				}

			}			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			client.getConnectionManager().shutdown();
		}

		return content;
	}
	
	public static NodeList parseHtml(String content,String charset,NodeFilter nodeFilter) {
		Parser parser = Parser.createParser(content, charset);
		// 通过过滤器过滤出<A>标签
		try {
			NodeList nodeList = parser.extractAllNodesThatMatch(nodeFilter);
//							new NodeFilter() {
//						
//						private static final long serialVersionUID = 1014260271053623538L;
//
//						// 实现该方法,用以过滤标签
//						@Override
//						public boolean accept(Node node) {							
//							if(node instanceof TableRow){
//								String classname=((TableRow)node).getAttribute("class");
//								if("greybg".equals(classname)){
//									return true;
//								}
//							}							
//							return false;
//						}
//					}
						
//			for (int i = 0; i < nodeList.size(); i++) {   
//				TableRow n = (TableRow) nodeList.elementAt(i);
//                TableColumn[] tds = n.getColumns();
//                int index =-1;
//                for(int j=0;j<tds.length;j++){
//                	TableColumn td = tds[j];
//                	if("公司简介".equals(td.getStringText())){
//                		index = j+1;
//                		break;
//                	}
//                }
//                if(index!=-1)System.out.println(tds[index].getStringText());
//            }   
			return nodeList;
		} catch (ParserException e) {			
			e.printStackTrace();
		}
		return null;
	}
}
