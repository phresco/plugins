package com.photon.phresco.plugins;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.plugin.commons.PluginConstants;
import com.photon.phresco.plugins.api.PhrescoPlugin;
import com.photon.phresco.util.Constants;
import com.phresco.pom.exception.PhrescoPomException;
import com.phresco.pom.util.PomProcessor;

/**
 * @author suresh_ma
 * @goal functional-test
 *
 */
public class PhrescoRunFunctionalTest extends PhrescoAbstractMojo implements PluginConstants {

	private static final String FUNCTIONAL_TEST = Constants.PHASE_FUNCTIONAL_TEST;
	
	/**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /**
     * @parameter expression="${project.basedir}" required="true"
     * @readonly
     */
    protected File baseDir;

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			File pomPath = new File(baseDir + File.separator + Constants.POM_NAME);
			PomProcessor processor = new PomProcessor(pomPath);
			String property = processor.getProperty(FUNCTIONAL_TEST_SELENIUM_TYPE);
			String goal = "";
			if (property.equals(WEBDRIVER)) {
				goal = FUNCTIONAL_TEST + HYPEN + WEBDRIVER.trim();
			} else if (property.equals(GRID)) {
				goal = FUNCTIONAL_TEST + HYPEN + GRID.trim();
			}
			String infoFile = baseDir + File.separator+ Constants.FUNCTIONAL_TEST_INFO_FILE;
			if (isGoalAvailable(infoFile, goal)&& getPluginName(infoFile, goal) != null) {
				PhrescoPlugin plugin = getPlugin(getPluginName(infoFile, goal));
				plugin.runFunctionalTest(getConfiguration(infoFile, goal), getMavenProjectInfo(project));
			} else {
				PhrescoPlugin plugin = new PhrescoBasePlugin(getLog());
				plugin.runFunctionalTest(getConfiguration(infoFile, goal), getMavenProjectInfo(project));
			}
		} catch (PhrescoException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (PhrescoPomException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}

	}
}
