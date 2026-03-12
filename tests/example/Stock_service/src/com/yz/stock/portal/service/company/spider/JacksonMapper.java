package com.yz.stock.portal.service.company.spider;

import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author wind
 * Jackson用于转换的核心类ObjectMapper无需每次都new一个object，官网上的一句话：can reuse, share globally
 */
public class JacksonMapper {

	private static final ObjectMapper mapper = new ObjectMapper();   
	  
    private JacksonMapper() {   
  
    }   
  
    public static ObjectMapper getInstance() {
        return mapper;   
    }  
}
