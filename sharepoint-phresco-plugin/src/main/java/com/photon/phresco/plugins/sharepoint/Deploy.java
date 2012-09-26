package com.photon.phresco.plugins.sharepoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.framework.PhrescoFrameworkFactory;
import com.photon.phresco.framework.api.ProjectAdministrator;
import com.photon.phresco.framework.model.SettingsInfo;
import com.photon.phresco.plugin.commons.MavenProjectInfo;
import com.photon.phresco.plugin.commons.PluginConstants;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration;
import com.photon.phresco.plugins.util.MojoUtil;
import com.photon.phresco.util.ArchiveUtil;
import com.photon.phresco.util.Constants;
import com.photon.phresco.util.Utility;
import com.photon.phresco.util.ArchiveUtil.ArchiveType;

public class Deploy implements PluginConstants {
	
	private static final String DOT_WSP = ".wsp";
	private File baseDir;
	private String buildName;
	private String environmentName;

	private File buildFile;
	private File tempDir;
	private File temp;
	private File build;
	private Log log;
	
	public void deploy(Configuration configuration, MavenProjectInfo mavenProjectInfo, Log log) throws PhrescoException {
		this.log = log;
		baseDir = mavenProjectInfo.getBaseDir();
        Map<String, String> configs = MojoUtil.getAllValues(configuration);
        environmentName = configs.get(ENVIRONMENT_NAME);
        buildName = configs.get(BUILD_NAME);
        
		try {
			init();
			extractBuild();
			deploy();
		} catch (MojoExecutionException e) {
			throw new PhrescoException(e);
		}
		
	}
	private void init() throws MojoExecutionException {
		try {

			if (StringUtils.isEmpty(buildName) || StringUtils.isEmpty(environmentName)) {
				callUsage();
			}
			File buildDir = new File(baseDir.getPath() + BUILD_DIRECTORY);
			build = new File(baseDir.getPath() + "\\source" + "\\");
			buildFile = new File(buildDir.getPath() + File.separator + buildName);
			tempDir = new File(buildDir.getPath() + TEMP_DIR);
			tempDir.mkdirs();
			temp = new File(tempDir.getPath() + "\\" + baseDir.getName() + DOT_WSP);
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void callUsage() throws MojoExecutionException {
		log.error("Invalid usage.");
		log.info("Usage of Deploy Goal");
		log.info(
				"mvn sharepoint:deploy -DbuildName=\"Name of the build\""
						+ " -DenvironmentName=\"Multivalued evnironment names\"");
		throw new MojoExecutionException("Invalid Usage. Please see the Usage of Deploy Goal");
	}

	private void extractBuild() throws MojoExecutionException {
		try {
			ArchiveUtil.extractArchive(buildFile.getPath(), tempDir.getPath(), ArchiveType.ZIP);
			FileUtils.copyFileToDirectory(temp, build);
			FileUtils.deleteDirectory(tempDir);
		} catch (PhrescoException e) {
			throw new MojoExecutionException(e.getErrorMessage(), e);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private void deploy() throws MojoExecutionException {
		try {
			ProjectAdministrator administrator = PhrescoFrameworkFactory.getProjectAdministrator();

			List<SettingsInfo> settingsInfos = administrator.getSettingsInfos(Constants.SETTINGS_TEMPLATE_SERVER,
					baseDir.getName(), environmentName);
			for (SettingsInfo serverDetails : settingsInfos) {
				String deployDirectory = serverDetails.getPropertyInfo(Constants.SERVER_DEPLOY_DIR).getValue();
				String serverContext = serverDetails.getPropertyInfo(Constants.SERVER_CONTEXT).getValue();
				String protocol = serverDetails.getPropertyInfo(Constants.SERVER_PROTOCOL).getValue();
				String host = serverDetails.getPropertyInfo(Constants.SERVER_HOST).getValue();
				String port = serverDetails.getPropertyInfo(Constants.SERVER_PORT).getValue();
				String projectCode = baseDir.getName();
				restore(protocol, deployDirectory, serverContext, host, port);
				addSolution(projectCode, deployDirectory);
				deploysolution(protocol, deployDirectory, serverContext, host, port, projectCode);
			}
		} catch (PhrescoException e) {
			throw new MojoExecutionException(e.getErrorMessage(), e);
		}
	}

	private void restore(String protocol, String deployDirectory, String serverContext, String host, String port) throws MojoExecutionException
	 {
		BufferedReader bufferedReader = null;
		boolean errorParam = false;
		try {
			File file = new File(build.getPath() + "\\phresco-pilot.dat");
			if (!file.exists()) {
				return;
			}
			StringBuilder sb = new StringBuilder();
			sb.append(SHAREPOINT_STSADM);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_O);
			sb.append(SHAREPOINT_RESTORE);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_URL);
			sb.append(STR_SPACE);
			sb.append(protocol);
			sb.append(SHAREPOINT_STR_COLON);
			sb.append(SHAREPOINT_STR_DOUBLESLASH);
			sb.append(host);
			sb.append(SHAREPOINT_STR_COLON);
			sb.append(port);
			sb.append(SHAREPOINT_STR_BACKSLASH);
			sb.append(serverContext);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_HYPEN);
			sb.append(SHAREPOINT_STR_OVERWRITE);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_HYPEN);
			sb.append(SHAREPOINT_STR_FILENAME);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_DOUBLEQUOTES + build.getPath() + "\\phresco-pilot.dat" + SHAREPOINT_STR_DOUBLEQUOTES);
			
			bufferedReader = Utility.executeCommand(sb.toString(), baseDir.getPath());
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.startsWith("[ERROR]")) {
					errorParam = true;
				}
			}
			if (errorParam) {
				throw new MojoExecutionException("Restore Failed ");
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			Utility.closeStream(bufferedReader);
		}
	}

	private void addSolution(String ProjectCode, String deployDirectory) throws MojoExecutionException {
		BufferedReader bufferedReader = null;
		boolean errorParam = false;
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(SHAREPOINT_STSADM);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_O);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_ADDSOLUTION);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_HYPEN);
			sb.append(SHAREPOINT_STR_FILENAME);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_DOUBLEQUOTES + baseDir.getPath() + "\\source" + "\\"
					+ ProjectCode + DOT_WSP + SHAREPOINT_STR_DOUBLEQUOTES);
			File file = new File(baseDir.getPath() + "\\source" + "\\"
					+ ProjectCode + DOT_WSP);
			if (file.exists()) {
				bufferedReader = Utility.executeCommand(sb.toString(), baseDir.getPath());
				String line = null;
				while ((line = bufferedReader.readLine()) != null) {
					if (line.startsWith("[ERROR]")) {
						errorParam = true;
					}
				}
				if (errorParam) {
					throw new MojoExecutionException("addSolution Failed");
				}
			} else {
				log.error("File Not found Exception");
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			Utility.closeStream(bufferedReader);
		}
	}

	private void deploysolution(String protocol, String deploydirectory, String serverContext, String host,
			String port, String projectCode) throws MojoExecutionException {
		BufferedReader bufferedReader = null;
		boolean errorParam = false;
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(SHAREPOINT_STSADM);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_O);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_DEPLOYSOLUTION);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_HYPEN);
			sb.append(SHAREPOINT_STR_NAME);
			sb.append(STR_SPACE);
			sb.append(projectCode + DOT_WSP);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_HYPEN);
			sb.append(SHAREPOINT_STR_URL);
			sb.append(STR_SPACE);
			sb.append(protocol);
			sb.append(SHAREPOINT_STR_COLON);
			sb.append(SHAREPOINT_STR_DOUBLESLASH);
			sb.append(host);
			sb.append(SHAREPOINT_STR_COLON);
			sb.append(port);
			sb.append(SHAREPOINT_STR_BACKSLASH);
			sb.append(serverContext);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_HYPEN);
			sb.append(SHAREPOINT_STR_IMMEDIATE);
			sb.append(STR_SPACE);
			sb.append(SHAREPOINT_STR_HYPEN);
			sb.append(SHAREPOINT_STR_ALLOWACDEP);
			bufferedReader = Utility.executeCommand(sb.toString(), baseDir.getPath());
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.startsWith("[ERROR]")) {
					errorParam = true;
				}
			}
			if (errorParam) {
				throw new MojoExecutionException("deploysolution Failed");
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			Utility.closeStream(bufferedReader);
		}
	}

}
