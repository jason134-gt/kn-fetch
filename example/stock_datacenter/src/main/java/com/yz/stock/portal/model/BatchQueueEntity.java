package com.yz.stock.portal.model;

public class BatchQueueEntity {

	int type;
	String tableName;
	Object v;
	
	public BatchQueueEntity(int type,String tableName ,Object v)
	{
		this.type = type;
		this.v = v;
		this.tableName = tableName;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	public <T>T getV() {
		if(v==null)
			return null;
		return (T) v;
	}

	public void setV(Object v) {
		this.v = v;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	
}
