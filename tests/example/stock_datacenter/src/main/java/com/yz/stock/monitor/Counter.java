package com.yz.stock.monitor;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.stock.common.util.DateUtil;
import com.stock.common.util.SMathUtil;
import com.yfzx.service.realtime.RealtimeDataItem;
import com.yz.common.vo.Pair;
import com.yz.configcenter.ConfigCenterFactory;

public class Counter {

	int addCount = 0;// 上涨分钟数
	int reduceCount = 0;// 下跌分钟数
	int lxAdd = 0;// 连续上涨数
	int lxxdAdd = 0;// 连续下跌数
	Double lrCje = 0.0;// 流入成交额
	Double lcCje = 0.0;// 流出成交额
	Double lr30 = 0.0;// 30分钟资金流入速度
	Double lr5 = 0.0;// 5分钟资金流入速度
	Double ls5 = 0.0;// 5分钟的平均流量
	Double ls10 = 0.0;// 10分钟的平均流量
	Double ls30 = 0.0;// 30分钟的平均流量
	Double clb = 0.0;// 当日虚拟量比,开盘到现在的量与前一日的量的比
	Long upTime = System.currentTimeMillis();// 记录的观察时间
	RealtimeDataItem item;
	Double zg = 0.0;// 最高价
	double zfn = 0.0;// n分钟内的涨跌幅
	Long zgUptime;// 创最高价的时间
	Long zgKeepTime;// 最高价保持时间,单位：分钟
	int curSeconds = 0;// 第多少分钟

	Double clb5 = 0.0;// 当日与5日均量的比
	Double clb10 = 0.0;// 当日与5日均量的比
	Double clb30 = 0.0;// 当日与5日均量的比

	// 最近一分钟这段时间内的量 与 5日内的平均流量的比
	double c1lb5 = 0.0;// 1分钟与5日均量的比
	double c1lb30 = 0.0;// 1分钟与30日均量的比

	double c3lb5 = 0.0;// 3分钟与5日均量的比
	double c3lb30 = 0.0;// 3分钟与30日均量的比

	double c5lb5 = 0.0;// 5分钟与5日均量的比
	double c5lb30 = 0.0;// 5分钟与30日均量的比
	// 瞬间量比，一到两分钟的量与30分钟的量的比
	Double sjLb130 = 0.0;// 1分钟与30分钟的量比
	Double sjLb330 = 0.0;// 3分钟与30分钟的量比
	Double sjLb530 = 0.0;// 5分钟与30分钟的量比

	Double zf5 = 0.0;// 5分钟涨幅
	Double zsu5 = 0.0;// 5分钟涨速

	Double zf7 = 0.0;// 7分钟涨幅
	Double zsu7 = 0.0;// 7分钟涨速

	Double zf10 = 0.0;// 10分钟涨幅
	Double zsu10 = 0.0;// 10分钟涨速

	Double zf15 = 0.0;// 15分钟涨幅
	Double zsu15 = 0.0;// 15分钟涨速

	Double zf30 = 0.0;// 30分钟涨幅
	Double zsu30 = 0.0;// 30分钟涨速

	Double arRatio = 0.0;// 涨跌比
	Double rcRatio = 0.0;// 流入流出比

	Integer ztSeconds = -1;// 涨停时间
	int ztstatus = 0;// 是否涨停过

	Map<String, Pair<Integer, Long>> _score = new HashMap<String, Pair<Integer, Long>>();

	public void checkZT(Double czf, RealtimeDataItem citem) {
		if (citem != null && czf > 9.97) {
			if(ztstatus==0)
			{
				ztstatus=1;
			}
			if ((ztSeconds == null || ztSeconds == -1) && czf > 9.9) {
				ztSeconds = curSeconds;
			}
		}
	}
	public void zTCount(Double czf, RealtimeDataItem citem) {
		if (citem != null && czf > 9.97) {
			if(ztstatus>0)
			{
				ztstatus++;
			}
		}
	}
	public void updataCount(RealtimeDataItem curitem, RealtimeDataItem peritem) {
		if (peritem == null) {
			ls5 = curitem.getCje();
		} else {
			Double cje = curitem.getCje() - peritem.getCje();
			if (curitem.getCzf() >= 0) {
				addCount++;
				lxAdd++;
				lxxdAdd = 0;
				lrCje += cje;
			} else {
				reduceCount++;
				lxAdd = 0;// 重新计数
				lxxdAdd++;
				lcCje += cje;
			}
		}
		upTime = curitem.getUptime();
		this.item = curitem;
	}

	public int getAddCount() {
		return addCount;
	}

	public void setAddCount(int addCount) {
		this.addCount = addCount;
	}

	public int getReduceCount() {
		return reduceCount;
	}

	public void setReduceCount(int reduceCount) {
		this.reduceCount = reduceCount;
	}

	public int getLxAdd() {
		return lxAdd;
	}

	public void setLxAdd(int lxAdd) {
		this.lxAdd = lxAdd;
	}

	public Double getLrCje() {
		return lrCje;
	}

	public void setLrCje(Double lrCje) {
		this.lrCje = lrCje;
	}

	public Double getLcCje() {
		return lcCje;
	}

	public void setLcCje(Double lcCje) {
		this.lcCje = lcCje;
	}

	public Double getLs5() {
		return ls5;
	}

	public void setLs5(Double ls5) {
		this.ls5 = ls5;
	}

	public Double getClb() {
		return clb;
	}

	public void setClb(Double clb) {
		this.clb = clb;
	}

	public Long getUpTime() {
		return upTime;
	}

	public void setUpTime(Long upTime) {
		this.upTime = upTime;
	}

	public RealtimeDataItem getItem() {
		return item;
	}

	public void setItem(RealtimeDataItem item) {
		this.item = item;
	}

	public Double getZg() {
		return zg;
	}

	public void setZg(Double zg) {
		this.zg = zg;
	}

	public Long getZgUptime() {
		return zgUptime;
	}

	public void setZgUptime(Long zgUptime) {
		if (zgKeepTime != null) {
			// 最高价保持了多少分钟
			long second = zgKeepTime / (60 * 1000);
			int limitZgKeepSecond = ConfigCenterFactory.getInt(
					"realtime_server.counter_limitZgKeepSecond", 30);
			if (second > limitZgKeepSecond) {
				addScore("zgKeepTime", 1);
			}
		}

		this.zgUptime = zgUptime;
	}

	public Long getZgKeepTime() {
		return zgKeepTime;
	}

	public void setZgKeepTime(Long zgKeepTime) {
		this.zgKeepTime = zgKeepTime;
	}

	public double getZfn() {
		return zfn;
	}

	public void setZfn(double zfn) {
		this.zfn = zfn;
	}

	public void addScore(String key, int i) {
		Pair<Integer, Long> p = _score.get(key);
		if (p == null) {
			p = new Pair<Integer, Long>(new Integer(0), this.upTime);
			_score.put(key, p);
		}
		p.first += i;
		p.second = this.upTime;

	}

	public void addScore(String key, int i, long experiedtime) {
		Pair<Integer, Long> p = _score.get(key);
		if (p == null) {
			p = new Pair<Integer, Long>(new Integer(0), this.upTime);
			_score.put(key, p);

			p.first += i;
			p.second = this.upTime;
		} else {
			if (this.upTime - p.second > experiedtime) {
				p.first += i;
				p.second = this.upTime;
			}
		}

	}

	public Double getLs10() {
		return ls10;
	}

	public void setLs10(Double ls10) {
		this.ls10 = ls10;
	}

	public Double getLs30() {
		return ls30;
	}

	public void setLs30(Double ls30) {
		this.ls30 = ls30;
	}

	public Counter clone() {
		Counter c = new Counter();
		c.setAddCount(addCount);
		c.setArRatio(arRatio);
		c.setC1lb30(c1lb30);
		c.setC1lb5(c1lb5);
		c.setC3lb30(c3lb30);
		c.setC5lb30(c5lb30);
		c.setC3lb5(c3lb5);
		c.setC5lb5(c5lb5);
		c.setClb(clb);
		c.setClb10(clb10);
		c.setClb30(clb30);
		c.setClb5(clb5);
		c.setCurSeconds(curSeconds);
		c.setItem(item);
		c.setLcCje(lcCje);
		c.setLr30(lr30);
		c.setLr5(lr5);
		c.setLrCje(lrCje);
		c.setLs10(ls10);
		c.setLs30(ls30);
		c.setLs5(ls5);
		c.setLxAdd(lxAdd);
		c.setLxxdAdd(lxxdAdd);
		c.setRcRatio(rcRatio);
		c.setReduceCount(reduceCount);
		c.setSjLb130(sjLb130);
		c.setSjLb330(sjLb330);
		c.setSjLb530(sjLb530);
		c.setUpTime(upTime);
		c.setZf10(zf10);
		c.setZf15(zf15);
		c.setZf30(zf30);
		c.setZf5(zf5);
		c.setZf7(zf7);
		c.setZfn(zfn);
		c.setZg(zg);
		c.setZgKeepTime(zgKeepTime);
		c.setZgUptime(zgUptime);
		c.setZsu10(zsu10);
		c.setZsu15(zsu15);
		c.setZsu30(zsu30);
		c.setZsu5(zsu5);
		c.setZsu7(zsu7);
		return c;
	}

	@Override
	public String toString() {
		return "Counter [addCount="
				+ addCount
				+ ", reduceCount="
				+ reduceCount
				+ ", lxAdd="
				+ lxAdd
				+ ", lxxdAdd="
				+ lxxdAdd
				+ ", lrCje="
				+ lrCje
				+ ", lcCje="
				+ lcCje
				+ ", lr30="
				+ lr30
				+ ", lr5="
				+ lr5
				+ ", ls5="
				+ ls5
				+ ", ls10="
				+ ls10
				+ ", ls30="
				+ ls30
				+ ", clb="
				+ clb
				+ ", upTime="
				+ upTime
				+ ", item="
				+ item
				+ ", zg="
				+ zg
				+ ", zfn="
				+ zfn
				+ ", zgUptime="
				+ zgUptime
				+ ", zgKeepTime="
				+ zgKeepTime
				+ ", curSeconds="
				+ curSeconds
				+ ", clb5="
				+ clb5
				+ ", clb10="
				+ clb10
				+ ", clb30="
				+ clb30
				+ ", c1lb5="
				+ c1lb5
				+ ", c1lb30="
				+ c1lb30
				+ ", c3lb5="
				+ c3lb5
				+ ", c3lb30="
				+ c3lb30
				+ ", c5lb5="
				+ c5lb5
				+ ", c5lb30="
				+ c5lb30
				+ ", sjLb130="
				+ sjLb130
				+ ", sjLb330="
				+ sjLb330
				+ ", sjLb530="
				+ sjLb530
				+ ", zf5="
				+ zf5
				+ ", zsu5="
				+ zsu5
				+ ", zf7="
				+ zf7
				+ ", zsu7="
				+ zsu7
				+ ", zf10="
				+ zf10
				+ ", zsu10="
				+ zsu10
				+ ", zf15="
				+ zf15
				+ ", zsu15="
				+ zsu15
				+ ", zf30="
				+ zf30
				+ ", zsu30="
				+ zsu30
				+ ", arRatio="
				+ arRatio
				+ ", rcRatio="
				+ rcRatio
				+ ", ztSeconds="
				+ ztSeconds
				+ ", ztstatus="
				+ ztstatus
				+ ", ls510="
				+ getLs510()
				+ ", ls530="
				+ getLs530()
				+ ", ls130="
				+ getLs130()
				+ ", lravg="
				+ getLrAvg()
				+ ", lr5="
				+ lr5
				+ ", lr30="
				+ lr30
				+ ", lrb530="
				+ getLrb530()
				+ ", lrb5="
				+ getLrb5()
				+ ", clp="
				+ clb
				+ ", curSeconds="
				+ curSeconds
				+ ", score="
				+ _score
				+ ", upTime="
				+ DateUtil.format2String(new Date(upTime))
				+ ", item="
				+ item
				+ ", zg="
				+ zg
				+ ", zgUptime="
				+ (zgUptime == null ? "" : DateUtil.format2String(new Date(
						zgUptime))) + ",zgKeepTime="
				+ (zgKeepTime == null ? "" : zgKeepTime) + "分钟]";
	}

	public Double getLs510() {
		if (ls10 != 0 && !ls10.isInfinite() && !ls10.isNaN())
			return SMathUtil.getDouble(ls5 / ls10, 2);
		return 0.0;
	}

	public Double getLs530() {
		if (ls30 != 0 && !ls30.isInfinite() && !ls30.isNaN())
			return SMathUtil.getDouble(ls5 / ls30, 2);
		return 0.0;
	}

	public Double getLs130() {
		if (ls30 != 0 && !ls30.isInfinite() && !ls30.isNaN())
			return SMathUtil.getDouble(ls10 / ls30, 2);
		return 0.0;
	}

	public void upZgKeepTime(long cuptime) {
		zgKeepTime = (cuptime - zgUptime) / (60 * 1000);

	}

	public int getLxxdAdd() {
		return lxxdAdd;
	}

	public void setLxxdAdd(int lxxdAdd) {
		this.lxxdAdd = lxxdAdd;
	}

	public Map<String, Pair<Integer, Long>> get_score() {
		return _score;
	}

	public void set_score(Map<String, Pair<Integer, Long>> _score) {
		this._score = _score;
	}

	public void addSeconds() {
		curSeconds++;
	}

	public Double getLr30() {
		return lr30;
	}

	public void setLr30(Double lr30) {
		this.lr30 = lr30;
	}

	public Double getLr5() {
		return lr5;
	}

	public void setLr5(Double lr5) {
		this.lr5 = lr5;
	}

	// 平均流入的流量
	public Double getLrAvg() {
		if (curSeconds == 0)
			return 0.0;
		return lrCje / curSeconds;
	}

	public int getCurSeconds() {
		return curSeconds;
	}

	public void setCurSeconds(int curSeconds) {
		this.curSeconds = curSeconds;
	}

	public Double getLrb530() {
		if (lr30 != 0)
			return SMathUtil.getDouble(lr5 / lr30, 2);
		return 0.0;
	}

	public Double getLrb5() {
		if (getLrAvg() != 0)
			return SMathUtil.getDouble(lr5 / getLrAvg(), 2);
		return 0.0;
	}

	public Pair<Integer, Long> getScore(String key, int defaultvalue,
			Long defaultUpTime) {
		Pair<Integer, Long> p = get_score().get(key);
		if (p == null)
			return new Pair<Integer, Long>(defaultvalue, defaultUpTime);
		return p;
	}

	public Double getClb5() {
		return clb5;
	}

	public void setClb5(Double clb5) {
		this.clb5 = clb5;
	}

	public Double getClb10() {
		return clb10;
	}

	public void setClb10(Double clb10) {
		this.clb10 = clb10;
	}

	public Double getClb30() {
		return clb30;
	}

	public void setClb30(Double clb30) {
		this.clb30 = clb30;
	}

	public double getC1lb5() {
		return c1lb5;
	}

	public void setC1lb5(double c1lb5) {
		this.c1lb5 = c1lb5;
	}

	public double getC1lb30() {
		return c1lb30;
	}

	public void setC1lb30(double c1lb30) {
		this.c1lb30 = c1lb30;
	}

	public double getC3lb5() {
		return c3lb5;
	}

	public void setC3lb5(double c3lb5) {
		this.c3lb5 = c3lb5;
	}

	public double getC3lb30() {
		return c3lb30;
	}

	public void setC3lb30(double c3lb30) {
		this.c3lb30 = c3lb30;
	}

	public double getC5lb5() {
		return c5lb5;
	}

	public void setC5lb5(double c5lb5) {
		this.c5lb5 = c5lb5;
	}

	public double getC5lb30() {
		return c5lb30;
	}

	public void setC5lb30(double c5lb30) {
		this.c5lb30 = c5lb30;
	}

	public Double getSjLb130() {
		return sjLb130;
	}

	public void setSjLb130(Double sjLb130) {
		this.sjLb130 = sjLb130;
	}

	public Double getSjLb330() {
		return sjLb330;
	}

	public void setSjLb330(Double sjLb330) {
		this.sjLb330 = sjLb330;
	}

	public Double getSjLb530() {
		return sjLb530;
	}

	public void setSjLb530(Double sjLb530) {
		this.sjLb530 = sjLb530;
	}

	public Double getZf5() {
		return zf5;
	}

	public void setZf5(Double zf5) {
		this.zf5 = zf5;
	}

	public Double getZsu5() {
		return zsu5;
	}

	public void setZsu5(Double zsu5) {
		this.zsu5 = zsu5;
	}

	public Double getZf7() {
		return zf7;
	}

	public void setZf7(Double zf7) {
		this.zf7 = zf7;
	}

	public Double getZsu7() {
		return zsu7;
	}

	public void setZsu7(Double zsu7) {
		this.zsu7 = zsu7;
	}

	public Double getZf10() {
		return zf10;
	}

	public void setZf10(Double zf10) {
		this.zf10 = zf10;
	}

	public Double getZsu10() {
		return zsu10;
	}

	public void setZsu10(Double zsu10) {
		this.zsu10 = zsu10;
	}

	public Double getArRatio() {
		return arRatio;
	}

	public void setArRatio(Double arRatio) {
		this.arRatio = arRatio;
	}

	public Double getRcRatio() {
		return rcRatio;
	}

	public void setRcRatio(Double rcRatio) {
		this.rcRatio = rcRatio;
	}

	public Integer getZtSeconds() {
		return ztSeconds;
	}

	public void setZtSeconds(Integer ztSeconds) {
		this.ztSeconds = ztSeconds;
	}

	public int getZtstatus() {
		return ztstatus;
	}

	public void setZtstatus(int ztstatus) {
		this.ztstatus = ztstatus;
	}

	public Double getZf15() {
		return zf15;
	}

	public void setZf15(Double zf15) {
		this.zf15 = zf15;
	}

	public Double getZsu15() {
		return zsu15;
	}

	public void setZsu15(Double zsu15) {
		this.zsu15 = zsu15;
	}

	public Double getZf30() {
		return zf30;
	}

	public void setZf30(Double zf30) {
		this.zf30 = zf30;
	}

	public Double getZsu30() {
		return zsu30;
	}

	public void setZsu30(Double zsu30) {
		this.zsu30 = zsu30;
	}

}
