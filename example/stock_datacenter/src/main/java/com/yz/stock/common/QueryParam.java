/**
 * 
 */
package com.yz.stock.common;

public class QueryParam {
	
	private static final int	DEFAULT_PAGE_LIMIT	= 50;

	private String				sort;

	
	private String				dir;

	
	private int					start				= 0;


	private int					limit				= DEFAULT_PAGE_LIMIT;
	
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
}
