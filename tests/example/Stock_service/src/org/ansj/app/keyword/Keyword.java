package org.ansj.app.keyword;

public class Keyword implements Comparable<Keyword> {
	private String name;
	private double score;
	private double idf;
	private int freq;
	private String natureStr;//词行,供KeyWordComputer 个性化使用

	public Keyword(String name, int docFreq, double weight,String natureStr) {
		this.name = name;
		this.idf = Math.log(10000 + 10000.0 / (docFreq + 1));
		this.score = idf * weight;
		freq++;
		this.natureStr = natureStr;
	}
	
	/**
	 * @deprecated 原始方法 建议弃用
	 * @param name
	 * @param docFreq
	 * @param weight
	 */
	public Keyword(String name, int docFreq, double weight) {
		this.name = name;
		this.idf = Math.log(10000 + 10000.0 / (docFreq + 1));
		this.score = idf * weight;
		freq++;
		this.natureStr = null;
	}
	

	public Keyword(String name, double score) {
		this.name = name;
		this.score = score;
		this.idf = score;
		freq++;
	}

	public void updateWeight(int weight) {
		this.score += weight * idf;
		freq++;
	}

	public int getFreq() {
		return freq;
	}

	@Override
	public int compareTo(Keyword o) {
		if (this.score < o.score) {
			return 1;
		} else {
			return -1;
		}

	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		if (obj instanceof Keyword) {
			Keyword k = (Keyword) obj;
			return k.name.equals(name);
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		if(natureStr == null){
			return name + "/" + score;// "="+score+":"+freq+":"+idf;
		}else{
			return name +"["+natureStr+ "]/" + score;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public String getNatureStr() {
		return natureStr;
	}

	public void setNatureStr(String natureStr) {
		this.natureStr = natureStr;
	}

}
