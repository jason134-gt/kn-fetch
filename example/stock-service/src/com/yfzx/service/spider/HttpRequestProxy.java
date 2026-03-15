package com.yfzx.service.spider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.SimpleHttpConnectionManager;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.common.utils.IOUtils;

/**
 * 
 * http://blog.csdn.net/gzsword/article/details/7493367
 */
public class HttpRequestProxy{

	//http://seanhe.iteye.com/blog/234759
	//让connectionmanager管理httpclientconnection时是否关闭连接
	private static boolean alwaysClose = true;
	static Logger log = LoggerFactory.getLogger(HttpRequestProxy.class);
	
	private HttpClient client = 
			new HttpClient(new SimpleHttpConnectionManager());
//			new HttpClient(new MultiThreadedHttpConnectionManager());

	public HttpClient getHttpClient(){
//		client.setConnectionTimeout(10000);
//		client.setTimeout(10000);
		//连接上时间
		client.getHttpConnectionManager().getParams().setConnectionTimeout(10000); 
		//响应时间
		client.getHttpConnectionManager().getParams().setSoTimeout(10000);  
		return client;
	}

	/**
	 * 用法：
	 * HttpRequestProxy hrp = new HttpRequestProxy();
	 * hrp.doRequest("http://www.163.com",null,null,"gbk");
	 * 
	 * @param url  请求的资源ＵＲＬ
	 * @param postData  POST请求时form表单封装的数据 没有时传null
	 * @param header   request请求时附带的头信息(header) 没有时传null
	 * @param encoding response返回的信息编码格式 没有时传null
	 * @return  response返回的文本数据
	 * @throws Exception 
	 */
	public String doRequest(String url,Map postData,Map header,String encoding) throws Exception{
		String responseString = null;
		//头部请求信息
		Header[] headers = null;
		if(header != null){
			Set entrySet = header.entrySet();
			int dataLength = entrySet.size();
			headers= new Header[dataLength];
			int i = 0;
			for(Iterator itor = entrySet.iterator();itor.hasNext();){
				Map.Entry entry = (Map.Entry)itor.next();
				headers[i++] = new Header(entry.getKey().toString(),entry.getValue().toString());
			}
		}
		//post方式
		if(postData!=null){
			PostMethod postRequest = new PostMethod(url.trim());
			if(headers != null){
				for(int i = 0;i < headers.length;i++){
					postRequest.setRequestHeader(headers[i]);
				}
			}
			Set entrySet = postData.entrySet();
			int dataLength = entrySet.size();
			NameValuePair[] params = new NameValuePair[dataLength];
			int i = 0;
			for(Iterator itor = entrySet.iterator();itor.hasNext();){
				Map.Entry entry = (Map.Entry)itor.next();
				params[i++] = new NameValuePair(entry.getKey().toString(),entry.getValue().toString());
			}
			postRequest.setRequestBody(params);
			try {
				responseString = this.executeMethod(postRequest,encoding);
			} catch (Exception e) {
				throw e;
			} finally{
				postRequest.releaseConnection();
//				((MultiThreadedHttpConnectionManager)this.getHttpClient().getHttpConnectionManager()).shutdown();
			}
		}
		//get方式
		if(postData == null){
			GetMethod getRequest = new GetMethod(url.trim());
			if(headers != null){
				for(int i = 0;i < headers.length;i++){
					getRequest.setRequestHeader(headers[i]);
				}
			}
			try {
				responseString = this.executeMethod(getRequest,encoding);
			} catch (Exception e) {
				e.printStackTrace();
				throw e;
			}finally{
				getRequest.releaseConnection();
//				((MultiThreadedHttpConnectionManager)this.getHttpClient().getHttpConnectionManager()).shutdown();
			}
		}		

		return responseString;
	}

	private String executeMethod(HttpMethod request, String encoding) throws Exception{
		String responseContent = null;
		InputStream responseStream = null;
		BufferedReader rd = null;
		try {
			this.getHttpClient().executeMethod(request);
			if(encoding != null && !"".equals(encoding)){				
//				responseStream = request.getResponseBodyAsStream();
//				rd = new BufferedReader(new InputStreamReader(responseStream,
//						encoding));
//				String tempLine = rd.readLine();
//				StringBuffer tempStr = new StringBuffer();
//				String crlf=System.getProperty("line.separator");
//				while (tempLine != null)
//				{
//					tempStr.append(tempLine);
//					tempStr.append(crlf);
//					tempLine = rd.readLine();
//				}
//				responseContent = tempStr.toString();
				responseStream = request.getResponseBodyAsStream();				
				Header h = request.getResponseHeader("Content-Encoding");
				String hValue = "";
				if(h !=null){
					hValue = h.getValue();
				}
				if(hValue.contains("gzip")|| hValue.contains("GZIP")){
					GZIPInputStream gzipstream = new GZIPInputStream(responseStream);
					responseContent = IOUtils.readStreamAsString(gzipstream, encoding);
					IOUtils.safeClose(responseStream);
				}
				else{
					responseContent = IOUtils.readStreamAsString(responseStream, encoding);
					IOUtils.safeClose(responseStream);
				}
			}else{
				encoding = getContentCharSet(request.getResponseHeader("Content-Type"));
				responseStream = request.getResponseBodyAsStream();
				responseContent = IOUtils.readStreamAsString(responseStream, encoding);
				IOUtils.safeClose(responseStream);
//				responseStream = request.getResponseBodyAsStream();
				//IOUtils.toString 死锁了
//				responseContent = IOUtils.toString(responseStream);
//				responseContent = request.getResponseBodyAsString();
			}
			Header locationHeader = request.getResponseHeader("location");
			//返回代码为302,301时，表示页面己经重定向，则重新请求location的url，这在
			//一些登录授权取cookie时很重要
			if (locationHeader != null) {				
				String redirectUrl = locationHeader.getValue();
				log.warn("重定向到"+redirectUrl);
				this.doRequest(redirectUrl, null, null,null);
			}
		} catch (Exception e) {
			log.error("executeMethod异常"+e.getMessage());
		} finally{
			if(rd != null)
				try {
					rd.close();
				} catch (IOException e) {
					throw new Exception(e.getMessage());
				}
			if(responseStream != null)
				try {
					responseStream.close();
				} catch (IOException e) {
					throw new Exception(e.getMessage());

				}
		}
		return responseContent;
	}


	/**
	 * 特殊请求数据,这样的请求往往会出现redirect本身而出现递归死循环重定向
	 * 所以单独写成一个请求方法
	 * 比如现在请求的url为：http://localhost:8080/demo/index.jsp
	 * 返回代码为302 头部信息中location值为:http://localhost:8083/demo/index.jsp
	 * 这时httpclient认为进入递归死循环重定向，抛出CircularRedirectException异常
	 * @param url
	 * @return
	 * @throws Exception 
	 */
	public String doSpecialRequest(String url,int count,String encoding) throws Exception{
		String str = null;
		InputStream responseStream = null;
		BufferedReader rd = null;
		GetMethod getRequest = new GetMethod(url);
		//关闭httpclient自动重定向动能
		getRequest.setFollowRedirects(false);
		try {

			this.client.executeMethod(getRequest);
			Header header = getRequest.getResponseHeader("location");
			if(header!= null){
				//请求重定向后的ＵＲＬ，count同时加1
				this.doSpecialRequest(header.getValue(),count+1, encoding);
			}
			//这里用count作为标志位，当count为0时才返回请求的ＵＲＬ文本,
			//这样就可以忽略所有的递归重定向时返回文本流操作，提高性能
			if(count == 0){
				getRequest = new GetMethod(url);
				getRequest.setFollowRedirects(false);
				this.client.executeMethod(getRequest);
				responseStream = getRequest.getResponseBodyAsStream();
				rd = new BufferedReader(new InputStreamReader(responseStream,
						encoding));
				String tempLine = rd.readLine();
				StringBuffer tempStr = new StringBuffer();
				String crlf=System.getProperty("line.separator");
				while (tempLine != null)
				{
					tempStr.append(tempLine);
					tempStr.append(crlf);
					tempLine = rd.readLine();
				}
				str = tempStr.toString();
			}

		} catch (Exception e) {
			log.error(e.getMessage());
		} finally{
			getRequest.releaseConnection();
			if(rd !=null)
				try {
					rd.close();
				} catch (IOException e) {
					throw new Exception(e.getMessage());
				}
			if(responseStream !=null)
				try {
					responseStream.close();
				} catch (IOException e) {
					throw new Exception(e.getMessage());
				}
		}
		return str;
	}
	
	
	protected String getContentCharSet(Header contentheader){
		String charset = null;
		if (contentheader != null) {
			HeaderElement[] values = contentheader.getElements();

			if (values.length == 1) {
				NameValuePair param = values[0].getParameterByName("charset");
				if (param != null){
					charset = param.getValue();
				}
			}
		}
		if (charset == null) {
			charset = "utf-8";
		}
		return charset;
	}

	public static void main(String[] args) {
		String url = "http://news2.gtimg.cn/lishinews.php?name=finance_news&symbol=sh900943&page=1&_du_r_t=0.7308782";
		Map<Object,Object> header = new HashMap<Object,Object>();		
		try {
			String str = new HttpRequestProxy().doRequest(url, null, header, null);
			System.out.println(str.length());
		} catch (Exception e) {			
			e.printStackTrace();
		}
	}
	
}

