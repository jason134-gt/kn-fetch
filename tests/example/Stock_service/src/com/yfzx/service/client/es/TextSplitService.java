package com.yfzx.service.client.es;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.ansj.app.keyword.KeyWordComputer;
import org.ansj.app.keyword.Keyword;
import org.ansj.dic.LearnTool;
import org.ansj.domain.Nature;
import org.ansj.domain.Term;
import org.ansj.library.UserDefineLibrary;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.commons.lang.StringUtils;

import com.stock.common.model.USubject;
import com.stock.common.util.LogSvr;
import com.stock.common.util.StringUtil;
import com.yfzx.service.db.USubjectService;
import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;
import com.yz.mycore.core.util.BaseUtil;

/**
 * @author Administrator http://www.nlpcn.org/demo
 *         http://nlpchina.github.io/ansj_seg/
 *         http://www.docin.com/p-335548702.html
 *         
 *         https://github.com/NLPchina/Word2VEC_java
 */
public class TextSplitService {

	private static TextSplitService instance = new TextSplitService();
	private static HashMap<String, Integer> configMap = new HashMap<String, Integer>();
	private static HashMap<String, String> aMap = new HashMap<String, String>();
	private static KeyWordComputer kwc = new KeyWordComputer(1000);
	//构建一个新词学习的工具类。这个对象。保存了所有分词中出现的新词。出现次数越多。相对权重越大。
	private static LearnTool learnTool = new LearnTool() ;

	private TextSplitService() {
	}

	public static TextSplitService getInstance() {
		return instance;
	}

	static {
		initConfig();
		//启动时候加载一下配置
		kwc.computeArticleTfidf("美的集团，大秦铁路，海康威视都是白马股。");
		ConfigCenterFactory.notifyRefresh(new IConfigRefresh() {
			public void refresh() {
				initConfig();
			}
		});
	}

	/**
	 * 
	 * 加载用户自定义词典和补充词典
	 */
	private static void initConfig() {
		UserDefineLibrary.clear();
		configMap.clear();
		aMap.clear();

		String config = ConfigCenterFactory
				.getString("stock_word.config",
						"company:2,industry:2,gainian:4,duo:3,qiangduo:6,kong:3,qiangkong:6");
		String[] configArr = config.split(",");
		for (int i = 0; i < configArr.length; i++) {
			String configStr = configArr[i];
			if (StringUtil.isEmpty(configStr)) {
				continue;
			}
			String key = configStr.split(":")[0];
			String level = configStr.split(":")[1];
			try {
				configMap.put(key, Integer.valueOf(level));
			} catch (Exception e) {
				configMap.put(key, 1);
			}
		}

		// 公司
		List<USubject> usList = USubjectService.getInstance()
				.getUSubjectListByType(0);
		for (USubject usubject : usList) {
			String name = usubject.getName();
			aMap.put(name, usubject.getUidentify());
			aMap.put(usubject.getUidentify(), usubject.getUidentify());
			String key = "company";
			int level = configMap.get(key) == null ? 1 : configMap.get(key);
			UserDefineLibrary.insertWord(name, key, 1000);
		}

		// 行业
		usList = USubjectService.getInstance().getUSubjectListByType(1);
		for (USubject usubject : usList) {
			String name = usubject.getName();
			String key = "industry";
			int level = configMap.get(key) == null ? 1 : configMap.get(key);
			UserDefineLibrary.insertWord(name, key, 1000);
		}

		// 概念 hq_2_3D打印
		usList = USubjectService.getInstance().getUSubjectListByType(2);
		for (USubject usubject : usList) {
			String name = usubject.getName();
			name = name.replace("hq_2_", "");
			String key = "gainian";
			int level = configMap.get(key) == null ? 1 : configMap.get(key);
			UserDefineLibrary.insertWord(name, key, 1000);
		}

		Set<String> keySet = configMap.keySet();

		for (String key : keySet) {
			int level = configMap.get(key) == null ? 1 : configMap.get(key);
			String keyWord = ConfigCenterFactory.getString("stock_word." + key,
					"");
			if (StringUtil.isEmpty(keyWord)) {
				continue;
			}
			String[] keyWordArr = keyWord.split(",");
			for (int j = 0; j < keyWordArr.length; j++) {
				if (StringUtil.isEmpty(keyWordArr[j])) {
					continue;
				}
				UserDefineLibrary.insertWord(keyWordArr[j], key, 1000);
			}
		}
	}

	public List<Term> splitWordFromContent(String content) {
		if (StringUtils.isBlank(content)) {
			return null;
		}
		return NlpAnalysis.parse(content);
	}

	/**
	 * 获取文章的看多看空状态，待完成
	 * @deprecated 
	 * @param title
	 * @param content
	 * @return
	 */
	public float computer(List<Keyword> kwList) {
		float f = 0f;
//		List<Term> terms = null;
//		if(StringUtil.isEmpty(title) == false ){
//			terms = ToAnalysis.parse(title);
//			for (Term term : terms) {
//				String key = term.getNatureStr();
//				Integer level = configMap.get(key);
//				if (level != null) {
//					f = f + level;
//				}
//			}
//		}
//		terms = ToAnalysis.parse(content);
//		for (Term term : terms) {
//			String key = term.getNatureStr();
//			Integer level = configMap.get(key);
//			if (level != null) {
//				f = f + level;
//			}
//		}
		return f;
	}

	public List<Keyword> getTagList(String title, String content) {
		Collection<Keyword> result =  null;
		if(StringUtil.isEmpty(title)){
			result = kwc.computeArticleTfidf(content);
		}else{
			result = kwc.computeArticleTfidf(title, content);
		}
		//机器学习
		getNewWord(title,content);
		
		if (result == null || result.size() == 0) {
			return null;
		}
		List<Keyword> kList = new ArrayList<Keyword>();
		for (Keyword keyword : result) {
			// int frep = keyword.getFreq();
			String natureStr = keyword.getNatureStr();
			if (configMap.get(natureStr) != null) {
				kList.add(keyword);
			}
		}
		return kList;
	}
	
	public void getNewWord(String title, String content){
		if( StringUtil.isEmpty(title) == false ){
			NlpAnalysis.parse(title, learnTool) ;					
		}
		NlpAnalysis.parse(content, learnTool) ;
	}
	
	
	public void logNewWord(){
		//取得学习到的topn新词,返回前10个。这里如果设置为0则返回全部
		List list =  learnTool.getTopTree(10000);
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[开始]\r\n");
		for(Object obj : list){
			sbuf.append(obj).append("\r\n");
		}
		sbuf.append("[结束]\r\n");
		String fileName = BaseUtil.getConfigPath("wstock/newword.log");
		try {
			LogSvr.logMsgWithoutDate(sbuf.toString(), fileName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void test() {
		long time = System.currentTimeMillis();
		String content = "美的集团是中国白色家电龙头股票，看涨到42，你觉得呢？还有深港通概念，应该没问题的。强烈推荐强烈推荐强烈推荐";		
		// System.out.println( (System.currentTimeMillis() - time) +"毫秒");
		// new NatureRecognition(terms).recognition(); //词性标注
		// System.out.println(terms);
		// System.out.println( (System.currentTimeMillis() - time) +"毫秒");

		String title = "中国中车澄清传言 否认将收购庞巴迪";
		String newContent = "中国中车中国中车近日，有媒体报道称中车收购中国置业为收购庞巴迪铺路，中国中车晚间发公告回应，称公司及公司下属子公司没有收购庞巴迪铁路业务的计划。周一中国中车继续大跌，"
				+ "截止收盘，报16.69元，跌幅8.60%，成交173.20亿。该股复牌后已腰斩。中国中车股份有限公司澄清公告原文本公司董事会及全体董事保证公告内容不存在任何虚假记载、"
				+ "误导性陈述或者重大遗漏，并对其内容真实、准确、完整承担个别及连带责任。近日，有媒体发表题为《中国中车收购中国置业为海外布局埋伏笔》、《中车收购中国置业为收购"
				+ "庞巴迪铺路？》的报道。中国中车股份有限公司（以下简称“公司”）对报道中涉及的事项进行了核查。一、 传闻情况上述报道称，公司全资附属公司CSR (HongKong)将认购"
				+ "中国置业投资新股份并取得绝对性控制权，此举是为了给收购庞巴迪铁路业务铺路。二、 澄清说明经核查，公司针对上述报道事项说明如下：1. 关于公司下属全资子公司中国南车"
				+ "(香港)有限公司（CSR (Hong Kong) Co. Limited）拟认购中国置业投资可能发行的新股的情况，请见公司刊发的日期为2015年6月19日的《中国中车股份有限公司"
				+ "关于南车香港公司可能认购交易的公告》。2. 截至目前，公司及公司下属子公司没有收购庞巴迪铁路业务的计划。三、 必要提示公司在此郑重提醒广大投资者：公司指定的信息披露"
				+ "报纸为《中国证券报》、《上海证券报》、《证券时报》、《证券日报》，公司指定的信息披露网站为上海证券交易所网站（www.sse.com.cn）和香港联合交易所有限公司网站"
				+ "(http://www. hkex.com.hk)。公司将严格按照有关法律法规的规定和要求开展信息披露工作。2公司发布的信息以在上述指定的信息披露报纸和网站刊登的公告为准，敬"
				+ "请广大投资者理性投资，注意投资风险。特此公告。中国中车股份有限公司董事会二〇一五年六月二十九日微信扫一扫，关注您的私人投资管家——腾讯证券公众号："
				+ "腾讯证券（qqzixuangu）";
	
		System.out.println(getTagList(title, newContent));
		System.out.println((System.currentTimeMillis() - time) + "毫秒");		
		
		System.out.println(getTagList("", content));
		System.out.println((System.currentTimeMillis() - time) + "毫秒");
	}

	public static void main(String[] args) {
		String words = "中国是世界四大文明古国之一，有着悠久的历史，距今约5000年前，以中原地区为中心开始出现聚落组织进而成国家和朝代，后历经多次演变和朝代更迭，持续时间较长的朝代有夏、商、周、汉、晋、唐、宋、元、明、清等。中原王朝历史上不断与北方游牧民族交往、征战，众多民族融合成为中华民族。20世纪初辛亥革命后，中国的君主政体退出历史舞台，取而代之的是共和政体。1949年中华人民共和国成立后，在中国大陆建立了人民代表大会制度的政体。中国有着多彩的民俗文化，传统艺术形式有诗词、戏曲、书法和国画等，春节、元宵、清明、端午、中秋、重阳等是中国重要的传统节日。";
		System.out.println(ToAnalysis.parse(words));
		System.out.println(TextSplitService.getInstance().splitWordFromContent(
				words));
		System.out.println(TextSplitService.getInstance().splitWordFromContent(
				words));
		KeyWordComputer kwc = new KeyWordComputer(5);
		// 增加新词,中间按照'\t'隔开
		// UserDefineLibrary.insertWord("ansj中文分词", "userDefine", 1000);
		// List<Term> terms = ToAnalysis.parse("我觉得Ansj中文分词是一个不错的系统!我是王婆!");
		// System.out.println("增加新词例子:" + terms);
		// // 删除词语,只能删除.用户自定义的词典.
		// UserDefineLibrary.removeWord("ansj中文分词");
		// terms = ToAnalysis.parse("我觉得ansj中文分词是一个不错的系统!我是王婆!");
		// System.out.println("删除用户自定义词典例子:" + terms);
		Collection<Keyword> result = kwc.computeArticleTfidf("", words);
		System.out.println(result);

		LearnTool learnTool = new LearnTool();

		// 进行词语分词。也就是nlp方式分词，这里可以分多篇文章
		NlpAnalysis.parse("说过，社交软件也是打着沟通的平台，让无数寂寞男女有了肉体与精神的寄托。", learnTool);
		NlpAnalysis.parse(
				"其实可以打着这个需求点去运作的互联网公司不应只是社交类软件与可穿戴设备，还有携程网，去哪儿网等等，订房订酒店多好的寓意",
				learnTool);
		NlpAnalysis.parse("张艺谋的卡宴，马明哲的戏", learnTool);

		// 取得学习到的topn新词,返回前10个。这里如果设置为0则返回全部
		System.out.println(learnTool.getTopTree(10));

		// 只取得词性为Nature.NR的新词
		System.out.println(learnTool.getTopTree(10, Nature.NR));
	}

}
