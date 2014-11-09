package hybridminer;

import java.util.HashSet;


public class Result {

	Itemset itemset; 
	Integer a;
	Integer b;
	HashSet<Integer> set = new HashSet<Integer>();

	public Itemset getItemset() {
		return itemset;
	}
	public void setItemset(Itemset itemset) {
		this.itemset = itemset;
	}
	public Integer getA() {
		return a;
	}

	public HashSet<Integer> getSet() {
		set = new HashSet<Integer>();
		set.add(a);
		set.add(b);
		for(int i = 0; i<itemset.getItems().length; i++){
			set.add(itemset.getItems()[i]);
		}
		return set;
	}

	public void setA(Integer a) {
		this.a = a;
	}
	public Integer getB() {
		return b;
	}
	public void setB(Integer b) {
		this.b = b;
	}

	public String toString(){
		return (a+ " "+b+" "+itemset);
	}


}
