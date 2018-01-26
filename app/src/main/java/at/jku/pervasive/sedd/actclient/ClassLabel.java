package at.jku.pervasive.sedd.actclient;

/**
 * Enumeration of all valid class labels.
 * 
 * @author matsch, 2012
 */
public enum ClassLabel {
	walking, sitting, standing;

	/**
	 * Convert parameter s to a class label. A case insensitive implementation
	 * of {@linkplain Enum#valueOf(Class, String)}.
	 * 
	 * @param s String
	 * @return ClassLabel or null if invalid
	 */
	public static ClassLabel parse(String s) {
		// "null" is interpreted as null class
		if (s.equalsIgnoreCase("null")) return null;
		// check if any enum constant name matches
		for (ClassLabel a : ClassLabel.class.getEnumConstants())
			if (a.name().equalsIgnoreCase(s)) return a;
		return null;
	}
}
