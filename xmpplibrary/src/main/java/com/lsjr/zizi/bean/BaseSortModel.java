package com.lsjr.zizi.bean;

public class BaseSortModel <T>{
	public T bean;
	public String firstLetter;// 首字母
	public String wholeSpell;// 全拼
	public String simpleSpell;// 首字母简拼
	public String getFirstLetter() {
		return firstLetter;
	}
	public void setFirstLetter(String firstLetter) {
		this.firstLetter = firstLetter;
	}
	public String getWholeSpell() {
		return wholeSpell;
	}
	public void setWholeSpell(String wholeSpell) {
		this.wholeSpell = wholeSpell;
	}
	public String getSimpleSpell() {
		return simpleSpell;
	}
	public void setSimpleSpell(String simpleSpell) {
		this.simpleSpell = simpleSpell;
	}
	public T getBean() {
		return bean;
	}
	public void setBean(T bean) {
		this.bean = bean;
	}
	
}
