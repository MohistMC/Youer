package gg.pufferfish.pufferfish.simd;

import java.util.logging.Logger;

@Deprecated
public class SIMDDetection {
	
	public static boolean isEnabled = false;
	public static boolean versionLimited = false;
	public static boolean testRun = false;
	
	@Deprecated
	public static boolean canEnable(Logger logger) {
		try {
			return SIMDChecker.canEnable(logger);
		} catch (NoClassDefFoundError | Exception ignored) {
			return false;
		}
	}
	
	@Deprecated
	public static int getJavaVersion() {
		// https://stackoverflow.com/a/2591122
		String version = System.getProperty("java.version");
		if(version.startsWith("1.")) {
			version = version.substring(2, 3);
		} else {
			int dot = version.indexOf(".");
			if(dot != -1) { version = version.substring(0, dot); }
		}
		version = version.split("-")[0]; // Azul is stupid
		return Integer.parseInt(version);
	}
	
}
