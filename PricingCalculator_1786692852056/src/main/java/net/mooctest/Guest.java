package net.mooctest;

public class Guest {
	private String name;
	private MemberLevel level;

	public Guest(String name, MemberLevel level) {
		this.name = name;
		this.level = level;
	}

	public String getName() { return name; }
	public MemberLevel getLevel() { return level; }
}