package com.yz.stock.portal.service.company.spider;

import java.io.*;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.stock.common.model.user.Members;

public class Test {
	private static final Logger log = LoggerFactory.getLogger(Test.class);
	// public static void main(String[] args) {
	// // String url =
	// "http://www.oschina.net/code/explore/achartengine/client/AndroidManifest.xml";
	// String url1 =
	// "http://www.windin.com/home/stock/stock-co/000002.sz.shtml";//"http://www.oschina.net/code/explore";
	// // String url2 = "http://www.oschina.net/code/explore/achartengine";
	// // String url3 =
	// "http://www.oschina.net/code/explore/achartengine/client";
	// // UrlQueue.addElem(url);
	// UrlQueue.addElem(url1);
	// // UrlQueue.addElem(url2);
	// // UrlQueue.addElem(url3);
	// UrlDataHanding[] url_Handings = new UrlDataHanding[1];
	// for (int i = 0; i < 1; i++) {
	// url_Handings[i] = new UrlDataHanding();
	// new Thread(url_Handings[i]).start();
	// }
	// }

	
	public String create100User(){
		
		String[] x = ("赵,钱,孙,李,周,吴,郑,王,冯,陈,褚,卫,蒋,沈,韩,杨,朱,秦,尤,许,何,吕,施,张,孔,曹,严,华,金,魏,陶,姜,戚,谢,邹," +
				"喻,柏,水,窦,章,云,苏,潘,葛,奚,范,彭,郎,鲁,韦,昌,马,苗,凤,花,方,俞,任,袁,柳,酆,鲍,史,唐,费,廉,岑,薛,雷,贺,倪,汤,滕," +
				"殷,罗,毕,郝,邬,安,常,乐,于,时,傅,皮,卞,齐,康,伍,余,元,卜,顾,孟,平,黄,和,穆,萧,尹,姚,邵,湛,汪,祁,毛,禹,狄,米,贝,明,臧," +
				"计,伏,成,戴,谈,宋,茅,庞,熊,纪,舒,屈,项,祝,董,梁,杜,阮,蓝,闵,席,季,麻,强,贾,路,娄,危,江,童,颜,郭,梅,盛,林,刁,钟,徐,邱," +
				"骆,高,夏,蔡,田,樊,胡,凌,霍,虞,万,支,柯,昝,管,卢,莫,柯,房,裘,缪,干,解,应,宗,丁,宣,贲,邓,郁,单,杭,洪,包,诸,左,石,崔,吉,钮," +
				"龚,程,嵇,邢,滑,裴,陆,荣,翁,荀,羊,于,惠,甄,曲,家,封,芮,羿,储,靳,汲,邴,糜,松,井,段,富,巫,乌,焦,巴,弓,牧,隗,山,谷,车,侯,宓,蓬," +
				"全,郗,班,仰,秋,仲,伊,宫,宁,仇,栾,暴,甘,钭,历,戎,祖,武,符,刘,景,詹,束,龙,叶,幸,司,韶,郜,黎,蓟,溥,印,宿,白,怀,蒲,邰,从,鄂,索,咸," +
				"籍,赖,卓,蔺,屠,蒙,池,乔,阳,郁,胥,能,苍,双,闻,莘,党,翟,谭,贡,劳,逄,姬,申,扶,堵,冉,宰,郦,雍,却,璩,桑,桂,濮,牛,寿,通,边,扈,燕,冀,浦," +
				"尚,农,温,别,庄,晏,柴,瞿,阎,充,慕,连,茹,习,宦,艾,鱼,容,向,古,易,慎,戈,廖,庾,终,暨,居,衡,步,都,耿,满,弘,匡,国,文,寇,广,禄,阙,东,欧," +
				"殳,沃,利,蔚,越,夔,隆,师,巩,厍,聂,晁,勾,敖,融,冷,訾,辛,阚,那,简,饶,空,曾,毋,沙,乜,养,鞠,须,丰,巢,关,蒯,相,查,后,荆,红,游,竺,权," +
				"逮,盍,益,桓,公,万俟,司马,上官,欧阳,夏侯,诸葛,闻人,东方,赫连,皇甫,尉迟,公羊,澹台,公冶,宗政,濮阳,淳于,单于,太叔,申屠,公孙,仲孙," +
				"轩辕,令狐,徐离,宇文,长孙,慕容,司徒,司空").split(",");//姓
		String[] nm = ("昌勋,昌盛,昌淼,昌茂,昌黎,昌燎,昌翰,晨朗,德明,德昌,德曜,范明,飞昂,高朗,高旻,晗日,晗昱,瀚玥,瀚昂,瀚彭,昊然,昊天,昊苍,昊英," +
				"昊宇,昊嘉,昊明,昊伟,昊硕,昊磊,昊东,鸿晖,鸿朗,华晖,金鹏,晋鹏,敬曦,景明,景天,景浩,景行,景中,景逸,景彰,景平,俊晖,君昊,昆琦,昆鹏,昆纬" +
				",昆宇,昆锐,昆卉,昆峰,昆颉,昆谊,昆皓,昆鹏,昆明,昆杰,昆雄,昆纶,鹏涛,鹏煊,绍晖,文昂,文景,曦哲,曦晨,曦之,新曦,鑫鹏,旭彬,旭尧,旭鹏," +
				"旭东,旭炎,炫明,宣朗,学智,轩昂,彦昌,曜坤,曜栋,曜文,曜曦,曜灿,曜瑞,永昌,子昂,智宇,智晖,智伟,智杰,智刚,智阳,朝旭,承悦,承允,承运,承载," +
				"承泽,承志,伟").split(",");//男名 
		String[] vm = ("朝雨,春芳,春华,春娇,春兰,春岚,春荷,春琳,春梅,春桃,春晓,春雪,春燕,春英,春妤,春姝,春晖,晗玥,晗蕾,晗蕊,晗晗,晗雨,晗琴,和暖," +
				"红旭,锦曦,晶晶,晶辉,晶灵,晶滢,晶霞,晶瑶,晶燕,晶茹,可昕,明洁,明明,明钰,明轩,明凝,明熙,明智,清晖,晴岚,晴雪,晴虹,晴波,晴霞,晴曦," +
				"晴丽,晴照,晴画,诗晗,素昕,添智,晓昕,晓畅,晓凡,晓枫,晓慧,晓兰,晓莉,晓曼,晓楠,晓彤,晓桐,晓星,晓燕,昕靓,昕葳,昕珏,昕月,昕雨,昕妤,旭辉," +
				"暄和,暄美,暄妍,暄文,暄婷,暄莹,暄嫣,暄玲,雪晴,雅晗,阳曦,曜儿,以晴,映颖,映秋,映雪,雨晴,月明,月朗,昭懿,梓暄,智美,智敏,智菱,婷," +
				"丹,芳,倩").split(",");
		for(int i=1;i<=100;i++){
			Members m = new Members();
			Random r = new Random();
			m.setEmail("test_"+i+"@163.com");			
			boolean gender = r.nextBoolean();
			m.setGender(gender);
			boolean single = Math.random()*3<1;
			String xStr = x[r.nextInt(x.length)];
			String mStr = "";
			if(gender == false){
				mStr = nm[r.nextInt(nm.length)];
			}else{
				mStr = vm[r.nextInt(vm.length)];
			}
			if(single == true){				
				mStr = mStr.length()==2?mStr.substring(1):mStr.substring(0);
			}
			String nick = xStr+mStr;
			log.info((gender?"女":"男")+" = "+nick);
			m.setNickname(nick);
		}
		return "";
	}
	
	public static void main(String[] args) {
		new Test().create100User(); 
	}
	

	/**
	 * 1. 演示将流中的文本读入一个 StringBuffer 中
	 * 
	 * @throws IOException
	 */
	public void readToBuffer(StringBuffer buffer, InputStream is)
			throws IOException {
		String line; // 用来保存每行读取的内容
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		line = reader.readLine(); // 读取第一行
		int i = 0;
		while (line != null) { // 如果 line 为空说明读完了
			i++;
			if(i%3!=0){
				
				if(i%3==2){
					buffer.append(line); // 将读到的内容添加到 buffer 中
					buffer.append("\n"); // 添加换行符
				}else{
					buffer.append(line.replaceAll("(0)*$",""));
					buffer.append(",");
				}
			}
			line = reader.readLine(); // 读取下一行
		}
		
	}
}
