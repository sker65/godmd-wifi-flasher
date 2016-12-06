package com.rinke.solutions.godmd.flash;

public class Pair<T1, T2> {
	public T1 left;
	public T2 right;
	public Pair(T1 left, T2 right) {
		super();
		this.left = left;
		this.right = right;
	}
	@Override
	public String toString() {
		return String.format("Pair [left=%s, right=%s]", left, right);
	}
}
