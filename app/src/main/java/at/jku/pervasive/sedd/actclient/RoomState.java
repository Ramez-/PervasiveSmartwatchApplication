package at.jku.pervasive.sedd.actclient;

/**
 * Enumeration of all valid room states.
 * 
 * @author matsch, 2012
 */
public enum RoomState {
	lecture, transition, empty;

	/**
	 * Convert parameter s to a class label. A case insensitive implementation
	 * of {@linkplain Enum#valueOf(Class, String)}.
	 * 
	 * @param s String
	 * @return ClassLabel or null if invalid
	 */
	public static RoomState parse(String s) {
		// "null" is interpreted as null class
		if (s.equalsIgnoreCase("null")) return null;
		// check if any enum constant name matches
		for (RoomState a : RoomState.class.getEnumConstants())
			if (a.name().equalsIgnoreCase(s)) return a;
		return null;
	}
}
