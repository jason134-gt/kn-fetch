package com.yfzx.service.ansj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ansj.app.keyword.KeyWordComputer;
import org.ansj.app.keyword.Keyword;
import org.ansj.domain.Term;
import org.ansj.splitWord.analysis.IndexAnalysis;
import org.apache.commons.lang.StringUtils;

public class KeywordsExtratorService {
	private KeywordsExtratorService() {}
	private static KeywordsExtratorService instance = new KeywordsExtratorService();
	public static KeywordsExtratorService getInstance() {
		return instance;
	}

	public List<Map.Entry<String, Integer>> extratorKeywordsEntryList(String content) {
		if(StringUtils.isBlank(content)) {
			return null;
		}
//		List<Term> termList = ToAnalysis.parse(content);
		List<Term> termList = IndexAnalysis.parse(content);
		if(termList != null && termList.size() > 0) {
			Map<String, Integer> map = new HashMap<String, Integer>();
			for(Term term : termList) {
				if(term.getRealName() != null && term.getRealName().length() < 2) {
					continue;
				}
				if(map.containsKey(term.getRealName())) {
					map.put(term.getRealName(), map.get(term.getRealName()) + 1);
				} else {
					map.put(term.getRealName(), 1);
				}
			}
		    List<Map.Entry<String, Integer>> entryList = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
		    Collections.sort(entryList,
	                new Comparator<Map.Entry<String, Integer>>() {
	                    public int compare(Entry<String, Integer> entry1,
	                            Entry<String, Integer> entry2) {

	                        return entry2.getValue() - entry1.getValue();
	                    }
	        });
		    return entryList;
		}
		return null;
	}

	public String extratorKeywords(String content, int limit) {
		List<Map.Entry<String, Integer>> entryList = extratorKeywordsEntryList(content);
		StringBuilder s = new StringBuilder();
		int counter = 0;
		if(entryList != null && entryList.size() > 0) {
			for(Map.Entry<String, Integer> entry : entryList) {
				s.append(entry.getKey()).append(",");
				counter++;
				if(counter >= limit) {
					break;
				}
			}
		}
		return s.toString();
	}

	public static void main(String[] args) {
		String content = "国家主席习近平在博鳌亚洲论坛开幕演讲3月29日，博鳌亚洲论坛年会落幕。当天，国家主席习近平同出席年会的中外企业家代表座谈，地点选在海南省博鳌国宾馆。40位中外企业负责人参加了座谈会。此前一天，习近平会见了美国盖茨基金会主席比尔・盖茨。地点同样是博鳌国宾馆。“希望企业家们继续关注和关心中国经济发展，把握中国机遇，同我们一道踏上中国发展新征程。”这是习近平对企业家们提出的“期望”。此次博鳌论坛，习近平见了哪些中外企业家？这些企业家又与中国有怎样的渊源？习近平又对他们提到了哪些“中国商机”？见了哪些企业家？企业家发言代表多为“论坛伙伴”。在与习近平主席的座谈会上，沙特基础工业公司等7家企业的负责人作为40位中外企业家代表进行了发言。习近平指出，当前世界经济复苏的不稳定、不确定、不均衡性依然突出，亚洲经济总体保持较快增长，中国经济发展步入新常态，在这样背景下，我愿意听取企业家们的意见。沙特基础工业公司是论坛首个战略合作伙伴北京青年报记者在博鳌亚洲论坛官方网站查询到，这7家企业多为“论坛伙伴”，包括沙特基础工业公司、澳大利亚福特斯克金属集团、德勤公司、海航集团、三星等。作为“论坛会员”的“正大”，其董事长谢国民也是发言代表之一。首位发言的是沙特基础工业公司董事长萨乌德亲王。沙特基础工业公司简称SABIC，是全球化学品、化肥、塑料及金属品的领先产商之一。2013年，SABIC在中国上海启用了创新中心，这是该公司在亚洲落成启用的两座全新创新中心之一。除上海外，还有一家位于印度班加罗尔。在SABIC亚太有限公司网站上，北青报记者查询到一篇刊发时间为3月27日的文章――《沙特基础工业公司献言博鳌亚洲论坛2015年年会：推进自由贸易，促进亚洲可持续发展》。文章表示，“作为博鳌亚洲论坛连续7年的支持者，SABIC对成为论坛14年历史上的首个战略合作伙伴而深感自豪。”说这句话的正是萨乌德亲王。座谈会上，萨乌德亲王认为，这次博鳌亚洲论坛年会非常成功，希望各方加强利益整合，推进亚洲命运共同体目标。澳大利亚福特斯克金属集团有一位中国非执行董事韩国三星集团副会长李在�在发言中谈到了中国的改革――“中共十八大以来，中国通过简政放权等措施深化改革，中国投资环境进一步改善，中国政府官员工作更加勤奋，中国成为世界最具竞争力的国家之一。”对于三星在中国的定位，李在�如是阐述――三星集团致力于建设扎根于中国本土的企业。澳大利亚福特斯克金属集团，简称FMG，是全球主要铁矿石供应商及海运铁矿石出口商。北青报记者在该公司官网上查询发现，FMG曾在一年内向中国客户出口铁矿石2700万吨。该公司官网的“董事会成员”一栏中，排名最后一位的非执行董事名叫曹慧泉。网站上如此介绍――按湖南华菱钢铁集团2009年2月25日FMG股票购买合约，华凌集团委任曹慧泉先生于2012年2月27日作为非执行董事加入FMG董事会。曹慧泉先生为华凌集团董事长。昨天的座谈会上，FMG董事长弗雷斯特表示，“我的集团在中国业务发展很快，同中国企业建立了良好合作关系，中国成为集团主要机械设备供应国”。谈及在中国的目标时，他表示“我们在中国的目标就是要开拓市场、收获友谊”。作为世界四大会计师事务所之一的德勤公司，其全球总裁阿尔蒙德在发言中谈到了“一带一路”：“我们在中国的业绩非常出色。我们致力于员工本土化，帮助中国制定会计标准，协助中国企业推进全球扩张计划，提供咨询服务。我们愿意继续为‘一带一路’建设，为中国建设发展提供支持。”作为发言代表之一的季姆琴科，是俄罗斯伏尔加资源公司创始人。他在发言中表示，“俄罗斯企业希望同中国进行更紧密的利益融合。当前两国工商界相互了解与合作不断深化，很多合作项目取得成功。俄企业对俄中关系发展前景充满信心，对俄中合作前景表示乐观。”4个月前的2014年11月，新华社曾刊发一篇名为《中俄企业家理事会俄方主席：俄中经贸合作迈上新台阶》的文章。文中写道：“中俄双边企业家理事会俄方主席根纳季・季姆琴科表示，今年以来俄罗斯与中国双边关系，包括两国经贸合作迈上了新台阶，两国签署的一系列大型合作协议将为双边经贸关系进一步发展注入强大动力。”泰国正大集团在改革开放之初来到中国泰国正大集团董事长谢国民在发言中表示，中国经济社会快速发展对发展中国家提供了重要启示，中国包括高铁在内的基础设施建设取得的巨大成就为国家发展发挥了关键作用。正大集团愿意投资泰国铁路建设，希望同中国加强在高铁技术、融资、建设方面合作，使这一合作成为“一带一路”建设的组成部分。谢国民，祖籍广东。正大集团是一家集农、工、商综合经营的国际性财团。在正大集团官网上，北青报记者查询到谢国民的“致辞”。文中说“1979年，和着改革开放的第一缕春风，正大集团来到了中国，很荣幸参与中国人民开创新时代的伟大实践。”“现在，中国正处于和平发展的战略机遇期”，“中华之崛起，也是全体炎黄子孙的共同心愿。”作为中国企业代表，占尽“地利”的海航集团董事长陈峰在发言中表示，民航业是“一带一路”建设的空中桥梁，“我们希望同政府加强沟通，加快深化民航体制改革，促进航空产业发展。”谈到哪些“商机”？习近平指出“绿色”等四大“中国商机”习近平在座谈会上表示，随着中国经济发展步入新常态，中外经济合作也在同步提升，意味着给世界各国及各国企业提供新的合作契机。中国将越来越开放，中国利用外资的政策不会变，对外商投资企业合法权益的保障不会变，为各国企业在华投资兴业提供更好服务的方向不会变。中国机遇的内涵在不断扩充在听取企业家发言后，习近平指出，中国经济深度融入全球经济，是亚洲乃至世界经济的重要驱动力。中国经济效益和质量正在提高，经济结构调整出现积极变化，深化改革开放取得重大进展，对外开放展现新局面，中国经济发展已经进入新常态，向形态更高级、分工更复杂、结构更合理阶段演化，这也是我们做好经济工作的出发点。在新常态下，要实现新发展、新突破，制胜法宝是全面深化改革，全面依法治国。习近平强调，当前，中国同世界的互动越来越紧密，机遇共享、命运与共的关系日益凸显，中国机遇的内涵在不断扩充.座谈会上，习近平还指出了市场、投资、绿色、合作等四大“商机”所在。――中国的市场机遇在扩大。中国在今后相当长时期仍然处于发展上升期，蕴含巨大内需。中国居民消费的拓展空间很大，提供安全优质产品和服务的企业会有良好发展前景。――中国的投资机遇在扩大。基础设施互联互通和新技术、新产品、新业态、新商业模式的投资机会正在不断涌现，新兴产业、服务业、小微企业作用更加凸显，小型化、智能化、专业化生产的发展催生更多机遇。――中国的绿色机遇在扩大。我们要走绿色发展道路，让资源节约、环境友好成为主流的生产生活方式。我们正在推进能源生产和消费革命，优化能源结构，落实节能优先方针，推动重点领域节能。――中国对外合作的机遇在扩大。我们支持多边贸易体制，致力于多哈回合谈判，倡导亚太自由贸易区，推进区域全面经济伙伴关系谈判，倡导筹建亚投行，全方位推进经济金融合作，作经济全球化和区域一体化的积极推动者。同沿线国家年贸易额有望破2.5万亿美元除此之外，习近平还强调，我们提出丝绸之路经济带和21世纪海上丝绸之路倡议，将促进中国与沿线国家的贸易与投资，促进沿线国家的互联互通与新型工业化，促进各国共同发展，人民共享发展成果。我们希望用10年左右的时间使中国同沿线国家的年贸易额突破2.5万亿美元。希望企业家朋友与中方各类企业、丝路基金和即将成立的亚洲基础设施投资银行加强对接，创新合作模式，共同探索开拓市场、互利共赢的新路子。希望企业家们继续关注和关心中国经济发展，把握中国机遇，同我们一道踏上中国发展新征程。此前一天，习近平在海南会见国际电信联盟秘书长赵厚麟时还曾提到另外一个“重大机遇”――中国积极参与国际电联工作，致力于同各成员国一道推进全球信息社会发展。希望国际电联抓住当前信息技术融合创新、快速发展的重大机遇，使国际电联更加开放高效、富有活力。赵厚麟感谢中国对国际电联工作的支持，表示国际电信联盟将不断加强同中国合作。文/本报记者岳菲菲供图/新华社习近平指出四大“中国商机”中国的市场机遇在扩大中国在今后相当长时期仍然处于发展上升期，蕴含巨大内需。中国的投资机遇在扩大基础设施互联互通和新技术、新产品、新业态、新商业模式的投资机会正在不断涌现。中国的绿色机遇在扩大我们正在推进能源生产和消费革命，优化能源结构，落实节能优先方针，推动重点领域节能。中国对外合作的机遇在扩大我们倡导筹建亚投行，全方位推进经济金融合作，作经济全球化和区域一体化的积极推动者。相关会见博鳌亚洲论坛第四届理事会成员习近平表示“一带一路”进入务实合作阶段“一带一路”领导小组办公室设在国家发改委国家主席习近平29日在海南省博鳌国宾馆会见博鳌亚洲论坛第四届理事会成员。倡导筹建亚投行迈出实质性步伐习近平指出，10多年来，博鳌亚洲论坛稳步发展，影响力不断提升，在凝聚亚洲共识、促进亚洲发展、提升亚洲影响力方面发挥了独特作用，也有力促进了中国同世界的友好交流合作。本次年会以“亚洲新未来：迈向命运共同体”为主题，探讨构建亚洲命运共同体，把握了亚洲发展的脉搏，对世界和平与发展也将具有深刻启示意义。习近平强调，“一带一路”倡议是新形势下中国扩大全方位开放的重要举措，也是中国着眼于深化区域经济合作提出的方案，致力于推动沿线国家共同发展。在各方共同努力下，“一带一路”建设开始进入务实合作阶段，一些早期收获项目已经成形，“一带一路”建设愿景和行动文件已经制定。日前，《推动共建丝绸之路经济带和21世纪海上丝绸之路的愿景与行动》授权发布。推进“一带一路”建设工作领导小组办公室负责人表示，领导小组办公室设在国家发展改革委，具体承担领导小组日常工作。中国将与沿线国家一道，不断充实完善“一带一路”的合作内容和方式，共同制定时间表、路线图，积极对接沿线国家发展和区域合作规划，签署合作框架协议和备忘录。会见哪些博鳌理事会成员博鳌亚洲论坛理事长福田康夫、巴基斯坦前总理阿齐兹、法国前总理拉法兰、马来西亚前总理巴达维、新西兰前总理希普利等先后发言。福田康夫表示，习近平主席在论坛开幕式上的演讲，从历史视角展开，谈到当今亚洲和世界大势，展现了亚洲团结和平发展的蓝图，为亚洲未来合作指明了方向。我们感谢中国政府对论坛长期以来的大力支持，论坛已成为各国领导人和工商界负责人，深入探讨解决经济社会发展面临的最紧迫问题的平台，有利于培养政府和企业、民间的信任关系。我们希望大家携手前行，坚定不移推进亚洲命运共同体建设。阿齐兹表示，习近平主席提出的“一带一路”倡议令人鼓舞，是实现和平繁荣的重要渠道，有利于加强本地区政治经济安全合作。亚投行将在诸多方面对世界带来重要和深远影响，我们要全力推动这一重要议程的落实。拉法兰表示，习近平主席提出重视创新，这对推动经济发展至关重要。世界在快速变化，发达国家和新兴经济体都需要创新增长，加强创新领域合作可以成为打造命运共同体的重要因素。各国要加强合作，集体创新。据新华社博鳌论坛新增加五位理事印尼前总统苏西洛新西兰前总理希普利巴基斯坦前总理阿齐兹泰国前副总理苏拉杰海南省常务副省长毛超峰";

		KeyWordComputer kwc = new KeyWordComputer(5);
	    String title = "习近平博鳌见了哪些企业家 指出四大“中国商机”";
//	    String title = "";
        Collection<Keyword> result = kwc.computeArticleTfidf(title, content);
        System.out.println(result);
//		String str = "1月13日早盘，丝绸之路概念涨幅居前，截至发稿，板块涨幅1.41%，个股方面，新疆城建领涨，股价报11.90元，涨幅6.31%；北新路桥涨逾6%；西部建设、友好集团、天上股份、青松建化等多股涨幅居前。消息面上，“一带一路”规划已经获批即将正式出台，新疆以及连云港两大起始点将率先突破。一名权威人士透露，规划已经印制完毕，在小范围下发，即将正式出台。作为跨越千年丝绸之路的新疆，更会得到格外关照。今年是新疆维吾尔自治区成立 60周年 ，围绕丝绸之路核心区建设，中央将给予新疆政策大礼包。该人士说。更多精彩内容，请关注腾讯证券 (微博) 微信公众号：qqzixuangu";
//	    List<Term> parse = BaseAnalysis.parse(str);
//		List<Term> parse1 = ToAnalysis.parse(str);

		List<Term> termList = IndexAnalysis.parse(content);
		if(termList != null && termList.size() > 0) {
			Map<String, Integer> map = new HashMap<String, Integer>();

			for(Term term : termList) {
				if(term.getRealName() != null && term.getRealName().length() < 2) {
					continue;
				}
				if(map.containsKey(term.getRealName())) {
					map.put(term.getRealName(), map.get(term.getRealName()) + 1);
				} else {
					map.put(term.getRealName(), 1);
				}
			}

		    List<Map.Entry<String, Integer>> entryList = new ArrayList<Map.Entry<String, Integer>>(map.entrySet());
		    Collections.sort(entryList,
	                new Comparator<Map.Entry<String, Integer>>() {
	                    public int compare(Entry<String, Integer> entry1,
	                            Entry<String, Integer> entry2) {

	                        return entry2.getValue() - entry1.getValue();
	                    }
	        });
		    System.out.println(entryList);

		}

	}
}
