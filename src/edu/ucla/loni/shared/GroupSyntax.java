/*
 * Copyright 2012 Michael Chang, Tai-Lin Chu, Artin Menachekanian,
 *                Charles Rudolph, Eduard Sedakov, Suzanna Whiteside
 * 
 * This file is part of ServerLibraryManager.
 *
 * ServerLibraryManager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ServerLibraryManager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ServerLibraryManager.  If not, see <http://www.gnu.org/licenses/>.
 */

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
	public static String agentToGroupname(String agent){
		return agent.substring(start.length(), agent.length() - end.length());
	}
	
	/**
	 *  Adds the groupSyntax to the name of a group
	 */
	public static String groupnameToAgent(String groupName){
		return start + groupName + end;
	}
}
