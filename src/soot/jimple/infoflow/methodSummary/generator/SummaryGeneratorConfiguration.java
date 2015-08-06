package soot.jimple.infoflow.methodSummary.generator;

import java.util.Set;

import soot.jimple.infoflow.InfoflowConfiguration;

/**
 * Configuration class for the data flow summary generator
 * 
 * @author Steven Arzt
 *
 */
public class SummaryGeneratorConfiguration extends InfoflowConfiguration {
	
	private boolean loadFullJAR = false;
	private Set<String> excludes = null;
	
	private int repeatCount = 1;

	/**
	 * Sets whether the target JAR file shall be loaded fully before the
	 * analysis starts. More precisely, this instructs StubDroid to not only
	 * explicitly load the target classes, but put the whole target JAR into
	 * Soot's process directory. This is, for instance, useful when analyzing
	 * all classes derived from a certain superclass. 
	 * @param loadFullJAR True if the target JAR file shall be fully loaded
	 * before performing the analysis, otherwise false.
	 */
	public void setLoadFullJAR(boolean loadFullJAR) {
		this.loadFullJAR = loadFullJAR;
	}
	
	/**
	 * Gets whether the target JAR file shall be loaded fully before the
	 * analysis starts. More precisely, this instructs StubDroid to not only
	 * explicitly load the target classes, but put the whole target JAR into
	 * Soot's process directory. This is, for instance, useful when analyzing
	 * all classes derived from a certain superclass. 
	 * @return True if the target JAR file shall be fully loaded before
	 * performing the analysis, otherwise false.
	 */
	public boolean getLoadFullJAR() {
		return this.loadFullJAR;
	}
	
	/**
	 * Sets the set of classes to be excluded from the analysis. Use pkg.* to
	 * exclude all classes in package "pkg"
	 * @param excludes The set of classes and packages to be excluded
	 */
	public void setExcludes(Set<String> excludes) {
		this.excludes = excludes;
	}
	
	/**
	 * Gets the set of classes to be excluded from the analysis.
	 * @return The set of classes and packages to be excluded
	 */
	public Set<String> getExcludes() {
		return this.excludes;
	}	
	
	/**
	 * Sets the number of time the analysis of every class shall be repeated.
	 * This is useful for measurements and evaluations.
	 * @param repeatCount The number of time the analysis of every class shall
	 * be repeated
	 */
	public void setRepeatCount(int repeatCount) {
		this.repeatCount = repeatCount;
	}

	/**
	 * Gets the number of time the analysis of every class shall be repeated.
	 * This is useful for measurements and evaluations.
	 * @return The number of time the analysis of every class shall be repeated
	 */
	public int getRepeatCount() {
		return this.repeatCount;
	}

}