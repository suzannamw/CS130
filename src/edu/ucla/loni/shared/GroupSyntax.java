package edu.ucla.loni.shared;

public class GroupSyntax {
	public static String start = "[";
	public static String end = "]";
	
	/**
	 * Returns true if the agent has the syntax required to be a group
	 */
	public static boolean isGroup(String agent){
		return agent.startsWith(start) && agent.endsWith(end);
	}
	
	/**
	 *  Trims off the group syntax to give you just the group name
	 */
	public static String agentToGroup(String agent){
		return agent.substring(start.length(), agent.length() - end.length());
	}
}
