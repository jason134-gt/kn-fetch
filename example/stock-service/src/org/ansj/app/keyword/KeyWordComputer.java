package org.ansj.app.keyword;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.nlpcn.commons.lang.util.StringUtil;

import com.yz.configcenter.ConfigCenterFactory;
import com.yz.configcenter.callback.IConfigRefresh;

public class KeyWordComputer {

	private static final Map<String, Double> POS_SCORE = new HashMap<String, Double>();

	static {		
		initConfig();		
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
		POS_SCORE.clear();
		POS_SCORE.put("null", 0.0);
		POS_SCORE.put("w", 0.0);
		POS_SCORE.put("en", 0.0);
		POS_SCORE.put("num", 0.0);
		POS_SCORE.put("nr", 3.0);
		POS_SCORE.put("nrf", 3.0);
		POS_SCORE.put("nw", 3.0);
		POS_SCORE.put("nt", 3.0);
		POS_SCORE.put("l", 0.2);
		POS_SCORE.put("a", 0.2);
		POS_SCORE.put("nz", 3.0);
		POS_SCORE.put("v", 0.2);

		String config = ConfigCenterFactory
				.getString("stock_word.config",
						"company:2,industry:2,gainian:4,duo:3,qiangduo:6,kong:3,qiangkong:6");
		String[] configArr = config.split(",");
		for (int i = 0; i < configArr.length; i++) {
			String configStr = configArr[i];
			if (StringUtil.isBlank(configStr)) {
				continue;
			}
			String key = configStr.split(":")[0];
			String level = configStr.split(":")[1];
			try {
				POS_SCORE.put(key, Double.valueOf(level));
			} catch (Exception e) {
				POS_SCORE.put(key, 1.0);
			}
		}
	}

	private int nKeyword = 5;

	public KeyWordComputer() {
	}

	/**
	 * 返回关键词个数
	 * 
	 * @param nKeyword
	 */
	public KeyWordComputer(int nKeyword) {
		this.nKeyword = nKeyword;

	}

	/**
	 * 
	 * @param content
	 *            正文
	 * @return
	 */
	private List<Keyword> computeArticleTfidf(String content, int titleLength) {
		Map<String, Keyword> tm = new HashMap<String, Keyword>();

		List<Term> parse = NlpAnalysis.parse(content);
		for (Term term : parse) {
			double weight = getWeight(term, content.length(), titleLength);
			if (weight == 0)
				continue;
			Keyword keyword = tm.get(term.getName());
			if (keyword == null) {
				keyword = new Keyword(term.getName(), term.natrue().allFrequency,weight,term.natrue().natureStr);
//				keyword = new Keyword(term.getName(), term.natrue().allFrequency, weight);
				tm.put(term.getName(), keyword);
			} else {
				keyword.updateWeight(1);
			}
		}

		TreeSet<Keyword> treeSet = new TreeSet<Keyword>(tm.values());

		ArrayList<Keyword> arrayList = new ArrayList<Keyword>(treeSet);
		if (treeSet.size() <= nKeyword) {
			return arrayList;
		} else {
			return arrayList.subList(0, nKeyword);
		}

	}

	/**
	 * 
	 * @param title
	 *            标题
	 * @param content
	 *            正文
	 * @return
	 */
	public List<Keyword> computeArticleTfidf(String title, String content) {
		if (StringUtil.isBlank(title)) {
			title = "";
		}
		if (StringUtil.isBlank(content)) {
			content = "";
		}
		return computeArticleTfidf(title + "\t" + content, title.length());
	}

	/**
	 * 只有正文
	 * 
	 * @param content
	 * @return
	 */
	public List<Keyword> computeArticleTfidf(String content) {
		return computeArticleTfidf(content, 0);
	}

	private double getWeight(Term term, int length, int titleLength) {
		if (term.getName().trim().length() < 2) {
			return 0;
		}

		String pos = term.natrue().natureStr;

		Double posScore = POS_SCORE.get(pos);

		if (posScore == null) {
			posScore = 1.0;
		} else if (posScore == 0) {
			return 0;
		}

		if (titleLength > term.getOffe()) {
			return 5 * posScore;
		}
		return (length - term.getOffe()) * posScore / (double) length;
	}

}
