package com.yz.stock.monitor;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang.StringUtils;

public class DeUtil {

	static int ed = 1;
	static DeUtil instance = new DeUtil();

	public DeUtil() {

	}

	public static DeUtil getInstance() {
		return instance;
	}

	public static void main(String[] args) {
		try {
			// String str =
			// "0:330:0:3:超跌反弹1号策略:cdft1hcl:486000000~1^0^0^$cp$<$zd$*1.005^1^0<1~0^1^2^$hpdl()>150 && $hexpcount(d,7,$hexp(${今日开盘价:4709}==${最近成交价:4717}&&${涨跌幅:4726}>9.96))==0 && (${向后复权后最近成交价:4742,-1} - ${向后复权后今日开盘价:4741,-1})/${向后复权后今日开盘价:4741,-2}*100>-9.9 && ${日实体:4824,-1}<0 && ${日实体:4824,-2}<0 && (${向后复权后最低成交价:4744,-1} - ${向后复权后今日开盘价:4741,-1})/${向后复权后今日开盘价:4741,-2}*100>-15 && (${向后复权后最低成交价:4744,-2} - ${向后复权后今日开盘价:4741,-2})/${向后复权后今日开盘价:4741,-3}*100>-15 && (${向后复权后今日开盘价:4741}-${向后复权后最近成交价:4742,-1})/${向后复权后最近成交价:4742,-1}*100>-4.1  && ${日量比:4827,-1}<1 && (1||$happear(d,15,$hexp(${涨跌幅:4726}>9.9))||( (${向后复权后最低成交价:4744,-2}<${日后复权30日均价:4866,-2}&& ${向后复权后最高成交价:4743,-2}>${日后复权30日均价:4866,-2} && ${向后复权后最低成交价:4744,-2}<${日后复权5日均价:4863,-2}&& ${向后复权后最高成交价:4743,-2}>${日后复权5日均价:4863,-2})==0))~2^2^($cp$*$hhqyz$-${向后复权后最高成交价:4743,-2})/${向后复权后最高成交价:4743,-2}*100<-20.01 && $cp$*$hhqyz$<${向后复权后最低成交价:4744,-1} && ($cp$*$hhqyz$-${向后复权后最近成交价:4742,-1})/${向后复权后最近成交价:4742,-1}*100<-4.1 && ($cp$*$hhqyz$ - ${向后复权后今日开盘价:4741})/${向后复权后今日开盘价:4741,-1}*100>-9.9 && ($cp$*$hhqyz$ - ${向后复权后今日开盘价:4741})/${向后复权后今日开盘价:4741,-1}*100>-15 && ($curSeconds$<30 && $clb$>1 && $clb$<5 ||  $curSeconds$>30 && $clb$<3 && $clb$>1)";
			// byte[] jiamiB = DeUtil.encrypt(str);
			// byte[] jiemiB2 = DeUtil.decrypt(jiamiB);
//			batchEncrypt("ztdk.txt");
			batchEncrypt("kpyd.txt");
			
//			String str = "0:31:0:4:开盘快速上涨:txlx0~1^0^0^$cp$>$zg$*0.999^1^$zf5$<-1.5~0^0^$cp$>$zg$*0.999~0^1^2^${涨跌幅:4726,-1}>9.9&& $hexpcount(d,2,$hexp(${今日开盘价:4709}==${最近成交价:4717}&&${涨跌幅:4726}>9.96))==0&&${涨跌幅:4726,-2}<6.1~2^2^$czf$>4.7  && $zsu5$>0.35";
//			byte[] jiamiB = DeUtil.encrypt(str);
//			System.out.println("encrypt str = " + new String(jiamiB,"ISO-8859-1"));
//
//			byte[] djiamiB = DeUtil.decrypt(new String(jiamiB,"ISO-8859-1").getBytes("ISO-8859-1"));
//			System.out.println("encrypt str = " + new String(djiamiB));
			
//			String str = "1;441;1;8;緩悝犔潁3搸讗薦;uymy5;597111112_1_1_2?1_2_1=3_3_%{utubuvt%?2!!''!%d{g%=:/:!''!%{uTfdpoet%=71!''!%{uTfdpoet%?1!''!%dvsTfdpoet%.%{uTfdpoet%?41!''!%{ifog%?1/2!''!%dmc%=1_2_3_%ibqqfbs)e-26-%ifyq)%|緩鸍溆;5837~?:/:**>>1'')%|瘦摏楎瞄6瘦枈弸;5974-.2~.%|瘦摏楎瞄41瘦枈弸;5977-.2~*0%|瘦摏楎瞄41瘦枈弸;5977-.2~=1/16!''!%ibqqfbs)e-8-%ifyq)%|緩鸍溆;5837~=.8/2**>>1!''!%iqem)*?261!''!%ifyqdpvou)e-8-%ifyq)%|弋瘦潁蜙弸;581:~>>%|睁���牑廥弸;5828~''%|緩鸍溆;5837~?:/:7**>>1!''!%|緩鸍溆;5837-.2~=6/2";
//			byte[] jiamiB = DeUtil.decrypt(str.getBytes());
//			System.out.println("encrypt str = " + new String(jiamiB));
			
//			double d = 705394935.16;
//			float f = BigDecimal.valueOf(d).floatValue();
//			System.out.println("encrypt str = " + f);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static byte[] decrypt(byte[] str) {
		byte[] data = str;
		for (int i = 0; i < data.length; i++) {
			int c = data[i];
			data[i] = (byte) (c - 1);
		}
		return data;
	}

	public static byte[] encrypt(String str) throws IllegalBlockSizeException,
			BadPaddingException, NoSuchAlgorithmException,
			NoSuchPaddingException {
		byte[] data = str.getBytes();
		for (int i = 0; i < data.length; i++) {
			int c = data[i];
			data[i] = (byte) (c + 1);
		}
		return data;
	}
	
	
	//初始化
		public static void batchEncrypt(String fpath) {
			
			try {
			        BufferedReader reader = null;
			        try {
			            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fpath),"UTF-8"));
			            String tempString = null;
			            // 一次读入一行，直到读入null为文件结束
			            while ((tempString = reader.readLine()) != null) {
//			                // 显示行号
//			                System.out.println("line " + line + ": " + tempString);
//			                line++;
			            	if(!StringUtils.isEmpty(tempString))
			            	{
			            		//为注释行，不处理
			            		if(tempString.startsWith("#"))
			            			continue;
			            		if(!tempString.contains("="))
			            			continue;
			            		String[] ka = tempString.split("=", 2);
			            		if(ka.length<2)
			            			continue;
			            		String k = ka[0];
			            		String v = ka[1];
			            		byte[] jiamiB = DeUtil.encrypt(v.trim());
			        			String result = new String(jiamiB,"ISO-8859-1").trim();
			        			System.out.println(k+"="+result);
			            	}
			            }
			            reader.close();
			        } catch (IOException e) {
			            e.printStackTrace();
			        } finally {
			            if (reader != null) {
			                try {
			                    reader.close();
			                } catch (IOException e1) {
			                }
			            }
			        }
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
}
