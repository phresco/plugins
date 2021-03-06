/**
 * Phresco Maven Plugin
 *
 * Copyright (C) 1999-2014 Photon Infotech Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.photon.phresco.plugins;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;

import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.plugin.commons.MavenProjectInfo;
import com.photon.phresco.plugins.api.PhrescoPlugin;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration;
import com.photon.phresco.plugins.util.MojoProcessor;
import com.photon.phresco.util.Constants;

/**
 * Goal which deploys the PHP project
 * 
 * @goal start
 * 
 */
public class PhrescoStart extends PhrescoAbstractMojo {

	private static final String START = Constants.PHASE_RUNGAINST_SRC_START;

	/**
	 * @parameter expression="${project.basedir}" required="true"
	 * @readonly
	 */
	protected File baseDir;

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/**
	 * @parameter expression="${phresco.project.code}" required="true"
	 */
	protected String projectCode;
	

    /**
     * @parameter expression="${moduleName}"
     * @readonly
     */
    protected String moduleName;
    
    /**
     * The current Maven session.
     *
     * @parameter default-value="${session}"
     * @parameter required
     * @readonly
     */
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    private BuildPluginManager pluginManager;
    
    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    
    /**@parameter 
     * default-value="${localRepository}" 
     * */
    private ArtifactRepository localRepository;
	
    /**
     * @parameter expression="${interactive}" required="true"
     * @readonly
     */
    private boolean interactive;
    
    /**
     * @parameter expression="${package.version}"
     * @readonly
     */
    protected String buildVersion;
    
    /**
     * <p>We can't autowire strongly typed RepositorySystem from Aether because it may be Sonatype (Maven 3.0.x)
     * or Eclipse (Maven 3.1.x/3.2.x) version, so we switch to service locator by autowiring entire {@link PlexusContainer}</p>
     *
     * <p>It's a bit of a hack but we have not choice when we want to be usable both in Maven 3.0.x and 3.1.x/3.2.x</p>
     *
     * @component
     * @required
     * @readonly
     */
     protected PlexusContainer container;
     
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			String dotPhrescoDirName = project.getProperties().getProperty(Constants.POM_PROP_KEY_SPLIT_PHRESCO_DIR);
        	if (StringUtils.isNotEmpty(dotPhrescoDirName)) {
        		baseDir = new File(baseDir.getParent() +  File.separatorChar + dotPhrescoDirName);
        	}
        	if (StringUtils.isNotEmpty(dotPhrescoDirName) && StringUtils.isNotEmpty(moduleName)) {
        		baseDir = new File(baseDir.getParentFile().getPath() +  File.separatorChar + dotPhrescoDirName);
        	}
			String infoFile = baseDir + File.separator + Constants.START_INFO_FILE;
			if (StringUtils.isNotEmpty(moduleName)) {
        		infoFile = baseDir + File.separator + moduleName + File.separator + Constants.START_INFO_FILE;
        	}
			if(! new File(infoFile).exists()) {
				System.out.println("RunAgainst Source Is Not Applicable For This Technology");
				return;
			}
			PhrescoPlugin plugin = getPlugin(getDependency(infoFile, START), mavenSession, project, container);
			Configuration configuration = getConfiguration(infoFile, START);
			MojoProcessor processor = new MojoProcessor(new File(infoFile));
			if(interactive) {
				configuration = getInteractiveConfiguration(configuration, processor, project, START);
			}
			MavenProjectInfo mavenProjectInfo = getMavenProjectInfo(project, moduleName, 
					mavenSession, pluginManager, localRepository);
			mavenProjectInfo.setBuildVersion(buildVersion);
			plugin.startServer(configuration, mavenProjectInfo);
		} catch (PhrescoException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
