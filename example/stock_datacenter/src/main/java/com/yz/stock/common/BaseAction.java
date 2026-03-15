package com.yz.stock.common;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.json.annotations.JSON;

import com.opensymphony.xwork2.ActionSupport;

public class BaseAction extends ActionSupport {

	
	private static final long serialVersionUID = -1128869791144359634L;

	
	private static final String	ERROR_MSG			= "msg";

	
	private static final String	ERROR_STACK			= "errorStack";

	
	private static final int	DEFAULT_PAGE_LIMIT	= 50; //默认值50,页面也定死了（GridViewPort.js:pageSize : 50,）,测试值10


	private boolean				success				= true;

	
	
	private Object			    resultData				= null;
	

	
	private Map					errorReason			= new HashMap();

	
	private int					start				= 0;


	private int					limit				= DEFAULT_PAGE_LIMIT;


	private int					totalCount			= 0;

	
	private String				sort;

	
	private String				dir;
	

	protected String				ids;  //批量删除id
	
	protected String className;//实体小写类名
	protected String sqlParam; //sql参数
	public final static String SESSION_MEMBERS = "AdminMembers";
	public final static String NOLOGIN = "NoLogin";//没有登录 自动调整到/NoLogin
//	@Resource(name = "searchManagerImpl")
//	private SearchManager searchManager;
	
	protected QueryParam queryParam = new QueryParam();



//	public String getSearchList(){
//		List mapList = searchManager.getSearchList(className, sqlParam,queryParam);
//		int count = searchManager.getSearchListCount(className, sqlParam);
//		this.setTotalCount(count); 
//		this.setResultData(mapList);
//		return "true";
//	}

	public final HttpSession getSession() {
		return getHttpServletRequest().getSession(true);
	}

	
	public final HttpServletRequest getHttpServletRequest() {
		HttpServletRequest request = ServletActionContext.getRequest();
		return request;
	}

	
	public final HttpSession getHttpSession() {
		HttpServletRequest request = ServletActionContext.getRequest();
		return request.getSession();
	}


	public final HttpServletResponse getHttpServletResponse() {
		HttpServletResponse response = ServletActionContext.getResponse();
		return response;
	}

	@JSON(name = "error")
	public Map getErrorReason() {
		return errorReason;
	}

	public void setErrorReason(Map errorReason) {
		this.errorReason = errorReason;
	}


	public void setErrorReason(String errorMsg) {
		if (errorReason == null) {
			errorReason = new HashMap();
		}

		setSuccess(false);
		this.errorReason.put(ERROR_MSG, errorMsg);
		this.errorReason.put(ERROR_STACK, "");
	}

	public void setErrorReason(String errorMsg, Exception e) {
		if (errorReason == null) {
			errorReason = new HashMap();
		}

		setSuccess(false);
		this.errorReason.put(ERROR_MSG, errorMsg);
		this.errorReason.put(ERROR_STACK, generateStackTrace(e));
	}

	public void injectQueryParam() {
		queryParam.setDir(getDir());
		queryParam.setLimit(getLimit());
		queryParam.setSort(getSort());
		queryParam.setStart(getStart());
	}
	



	private String generateStackTrace(Exception e) {
		if (e == null) {
			return null;
		}
		StringBuffer stringBuffer = new StringBuffer();
		ByteArrayOutputStream byteArrayOutputStream = null;
		try {
			byteArrayOutputStream = new ByteArrayOutputStream();
			e.printStackTrace(new PrintStream(byteArrayOutputStream));
			stringBuffer.append(byteArrayOutputStream.toString());
		}
		catch (Exception ex) {
		}
		finally {
			if (byteArrayOutputStream != null) {
				try {
					byteArrayOutputStream.close();
				}
				catch (IOException ex2) {
				}
			}
		}
		return stringBuffer.toString();
	}

	public int getStart() {
		return start;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}
	@JSON(name = "data")
	public Object getResultData() {
		return resultData;
	}
    
	public void setResultData(Object resultData) {
		this.resultData = resultData;
	}
	
	public String getIds() {
		return ids;
	}

	public void setIds(String ids) {
		this.ids = ids;
	}


	public String getClassName() {
		return className;
	}


	public void setClassName(String className) {
		this.className = className;
	}


	public String getSqlParam() {
		return sqlParam;
	}


	public void setSqlParam(String sqlParam) {
		this.sqlParam = sqlParam;
	}
}
