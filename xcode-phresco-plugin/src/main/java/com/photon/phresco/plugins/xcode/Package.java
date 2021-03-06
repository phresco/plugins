/**
 * xcode-phresco-plugin
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
package com.photon.phresco.plugins.xcode;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import com.google.gson.Gson;
import com.photon.phresco.commons.FrameworkConstants;
import com.photon.phresco.commons.model.ApplicationInfo;
import com.photon.phresco.commons.model.BuildInfo;
import com.photon.phresco.commons.model.ProjectInfo;
import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.framework.PhrescoFrameworkFactory;
import com.photon.phresco.framework.api.ActionType;
import com.photon.phresco.framework.api.ApplicationManager;
import com.photon.phresco.framework.api.ProjectManager;
import com.photon.phresco.plugin.commons.MavenProjectInfo;
import com.photon.phresco.plugin.commons.PluginConstants;
import com.photon.phresco.plugin.commons.PluginUtils;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter.MavenCommands.MavenCommand;
import com.photon.phresco.plugins.util.MojoProcessor;
import com.photon.phresco.plugins.util.MojoUtil;
import com.photon.phresco.util.Constants;
import com.photon.phresco.util.Utility;
import com.phresco.pom.exception.PhrescoPomException;
import com.phresco.pom.util.PomProcessor;

public class Package implements PluginConstants {
	

	private String environmentName;
	private Log log;
	private File dotPhrescoDir;
	private String dotPhrescoDirName;
	private File srcDir;
	private String srcDirName;
	/**
	 * Execute the xcode command line utility.
	 * @throws PhrescoException 
	 */
	public void pack(Configuration config, MavenProjectInfo mavenProjectInfo, Log log) throws PhrescoException {
	    BufferedReader projectInfoReader = null;
		try {
		    this.log = log;
		    
		    File baseDir = mavenProjectInfo.getBaseDir();
		    MavenProject project = mavenProjectInfo.getProject();
		    dotPhrescoDirName = project.getProperties().getProperty(Constants.POM_PROP_KEY_SPLIT_PHRESCO_DIR);
		    dotPhrescoDir = baseDir;
        	if (StringUtils.isNotEmpty(dotPhrescoDirName)) {
        		dotPhrescoDir = new File(baseDir.getParent() + File.separatorChar + dotPhrescoDirName);
        	}
        	PluginUtils utils = new PluginUtils();
        	ApplicationInfo appInfo = utils.getAppInfo(dotPhrescoDir);
        	String appDirName = appInfo.getAppDirName();
        	srcDirName = project.getProperties().getProperty(Constants.POM_PROP_KEY_SPLIT_PHRESCO_DIR);
        	srcDir = baseDir;
        	if (StringUtils.isNotEmpty(srcDirName)) {
        		srcDir = new File(Utility.getProjectHome() + File.separatorChar + appDirName + File.separatorChar + srcDirName);
        	}
           	String pomFile = project.getFile().getName();
            Map<String, String> configs = MojoUtil.getAllValues(config);
            environmentName = configs.get(ENVIRONMENT_NAME);
            String buildName = configs.get(BUILD_NAME);
            String buildNumber = configs.get(BUILD_NUMBER);
            String sdk = configs.get(SDK);
            String target = configs.get(TARGET);
            if (StringUtils.isNotEmpty(target)) {
            	target = target.replace(STR_SPACE, SHELL_SPACE);
            }
            String configuration = configs.get(MODE);
            String encrypt = configs.get(ENCRYPT);
            String plistFile = configs.get(PLIST_FILE);
            String projectType = configs.get(PROJECT_TYPE);
            PluginUtils.checkForConfigurations(dotPhrescoDir, environmentName);
            
		    StringBuilder projectInfoFile = new StringBuilder(dotPhrescoDir.getPath())
		    .append(File.separator)
		    .append(Constants.DOT_PHRESCO_FOLDER)
		    .append(File.separator)
		    .append(Constants.PROJECT_INFO_FILE);
		    Gson gson = new Gson();
		    projectInfoReader = new BufferedReader(new FileReader(new File(projectInfoFile.toString())));
		    ProjectInfo projectInfo = gson.fromJson(projectInfoReader, ProjectInfo.class);
		    ApplicationInfo applicationInfo = projectInfo.getAppInfos().get(0);
		    String embedAppId = applicationInfo.getEmbedAppId();
		    if (StringUtils.isNotEmpty(embedAppId)) {
		        embedApplication(baseDir, projectInfo, embedAppId, pomFile);
		    }
		    
		    StringBuilder sb = new StringBuilder();
			sb.append(XCODE_BUILD_COMMAND);
			
			sb.append(STR_SPACE);
			sb.append(HYPHEN_D + PROJECT_TYPE + EQUAL + projectType);
			
			if (StringUtils.isNotEmpty(environmentName)) {
				sb.append(STR_SPACE);
				sb.append(HYPHEN_D + ENVIRONMENT_NAME + EQUAL + environmentName);
			}
			
			if (StringUtils.isNotEmpty(sdk)) {
				sb.append(STR_SPACE);
				sb.append(HYPHEN_D + SDK + EQUAL + sdk);
			}
			
			if (StringUtils.isNotEmpty(target)) {
				sb.append(STR_SPACE);
				sb.append(HYPHEN_D + TARGET_NAME + EQUAL + target);
			}
			
			sb.append(STR_SPACE);
			sb.append(HYPHEN_D + CONFIGURATION + EQUAL + configuration);
			
			sb.append(STR_SPACE);
			sb.append(HYPHEN_D + ENCRYPT + EQUAL + encrypt);
			
			sb.append(STR_SPACE);
			sb.append(HYPHEN_D + PLIST_FILE + EQUAL + plistFile);
			
			if (StringUtils.isNotEmpty(buildName)) {
				sb.append(STR_SPACE);
				sb.append(HYPHEN_D + BUILD_NAME + EQUAL + buildName);
			}
			
			if (StringUtils.isNotEmpty(buildNumber)) {
				sb.append(STR_SPACE);
				sb.append(HYPHEN_D + BUILD_NUMBER + EQUAL + buildNumber);
			}
			if(!Constants.POM_NAME.equals(pomFile)) {
				sb.append(STR_SPACE);
				sb.append(Constants.HYPHEN_F);
				sb.append(STR_SPACE);
				sb.append(pomFile);
			}
			List<Parameter> parameters = config.getParameters().getParameter();
			for (Parameter parameter : parameters) {
				if(parameter.getPluginParameter() != null && parameter.getMavenCommands() != null) {
					List<MavenCommand> mavenCommands = parameter.getMavenCommands().getMavenCommand();
					for (MavenCommand mavenCommand : mavenCommands) {
						if(parameter.getValue().equals(mavenCommand.getKey())) {
						    sb.append(STR_SPACE);
							sb.append(mavenCommand.getValue());
						}
					}
				}
			}			
			
	       boolean status = Utility.executeStreamconsumer(sb.toString(), baseDir.getPath(), baseDir.getPath(), "");
			if(!status) {
				try {
					throw new MojoExecutionException(Constants.MOJO_ERROR_MESSAGE);
				} catch (MojoExecutionException e) {
					throw new PhrescoException(e);
				}
			} else {
				String appBuildName = "";
				String deployLocation = "";
				String appDirectory = baseDir.getPath();
				String buildInfoFilePath = appDirectory.concat(BUILD_DIRECTORY).concat(File.separator).concat("build.info");
				ApplicationManager applicationManager = PhrescoFrameworkFactory.getApplicationManager();
				List<BuildInfo> buildInfos = applicationManager.getBuildInfos(new File(buildInfoFilePath));
				if (CollectionUtils.isNotEmpty(buildInfos)) {
					Collections.sort(buildInfos, new BuildComparator());
					Map<String, Boolean> options = buildInfos.get(0).getOptions();
					if (MapUtils.isNotEmpty(options) && options.get("canCreateIpa")) {
					deployLocation = buildInfos.get(0).getDeployLocation();
					String buildNameSubstring = deployLocation.substring(0, deployLocation.lastIndexOf( File.separator));
					appBuildName = buildNameSubstring.substring(buildNameSubstring.lastIndexOf( File.separator) + 1);
					String ipaFileName = applicationInfo.getName();
					StringBuilder testFlightCmd = new StringBuilder("mvn xcode:ipaBuilder ");
					testFlightCmd.append("-Dapplication.name=" + ipaFileName)
					.append(" -Dapp.path=" + deployLocation)
					.append(" -Dbuild.name=" + appBuildName);
					Utility.executeStreamconsumer(testFlightCmd.toString(), baseDir.getPath(), baseDir.getPath(), "");
					}
				}
			}
		} catch (FileNotFoundException e) {
			throw new PhrescoException(e);
		} finally {
            if (projectInfoReader != null) {
                try {
                    projectInfoReader.close();
                } catch (IOException e) {
                    throw new PhrescoException(e);
                }
            }
        }
		

	}

    private void embedApplication(File baseDir, ProjectInfo projectInfo, String embedAppId, String pom) throws PhrescoException {
        try {
        ProjectManager projectManager = PhrescoFrameworkFactory.getProjectManager();
        ProjectInfo embedProject = projectManager.getProject(projectInfo.getId(), projectInfo.getCustomerIds().get(0), embedAppId);
        ApplicationInfo embedAppInfo = embedProject.getAppInfos().get(0);
        String embedBaseDir = Utility.getProjectHome() + embedAppInfo.getAppDirName();
        StringBuilder packageInfoFile = new StringBuilder(embedBaseDir)
        .append(File.separator)
        .append(Constants.PACKAGE_INFO_FILE);
        MojoProcessor processor = new MojoProcessor(new File(packageInfoFile.toString()));
        Parameter parameter = processor.getParameter(Constants.MVN_GOAL_PACKAGE, Constants.MOJO_KEY_ENVIRONMENT_NAME);
        parameter.setValue(environmentName);
        processor.save();
        
        ApplicationManager applicationManager = PhrescoFrameworkFactory.getApplicationManager();
        List<String> buildArgCmds = new ArrayList<String>();
        String pomFileName = Utility.getPomFileName(embedAppInfo);
		if(!Constants.POM_NAME.equals(pomFileName)) {
			buildArgCmds.add(Constants.HYPHEN_F);
			buildArgCmds.add(pomFileName);
		}
        BufferedInputStream reader = applicationManager.performAction(embedProject, ActionType.BUILD, null, embedBaseDir);
		int available = reader.available();
		while (available != 0) {
			byte[] buf = new byte[available];
            int read = reader.read(buf);
            if (read == -1 ||  buf[available-1] == -1) {
            	break;
            } else {
            	String line = new String(buf);
            	System.out.println(line);
            }
            available = reader.available();
		}

        File pomFile = new File(baseDir.getPath() + File.separator + pom);
        PomProcessor pomProcessor = new PomProcessor(pomFile);
        String appTargetProp = pomProcessor.getProperty(Constants.POM_PROP_KEY_EMBED_APP_TARGET_DIR);
        if(StringUtils.isEmpty(appTargetProp)) {
            throw new PhrescoException("Target directory to embed the selected application is not specified");
        }
        String appTargetDir = srcDir + File.separator + appTargetProp;
        File[] wwwFiles = new File(appTargetDir).listFiles();
        if (!ArrayUtils.isEmpty(wwwFiles)) {
            for (File file : wwwFiles) {
                file.delete();
            }
        }
        String source = embedBaseDir + File.separator + Constants.DO_NOT_CHECKIN_DIRY + File.separator + "target";
        File[] listFiles = new File(source).listFiles();
        String fileName = "";
        for (File file : listFiles) {
            if (file.getName().endsWith(".war")) {
                source = source + File.separator + file.getName();
                fileName = file.getName();
                break;
            }
        }
        File src = new File(source);
        File dest = new File(appTargetDir + File.separator + fileName);
        FileUtils.copyFile(src, dest);
        System.out.println("[info] Extracting the war");
        BufferedReader warExtractRdr = Utility.executeCommand("jar xvf " + fileName, appTargetDir);
        while (warExtractRdr.readLine() != null) {
            System.out.println(warExtractRdr.readLine());
        }
        System.out.println("[info] war extracted successfully...");
        dest.delete();
        } catch (PhrescoException e) {
            throw new PhrescoException(e);
        } catch (IOException e) {
            throw new PhrescoException(e);
        } catch (PhrescoPomException e) {
            throw new PhrescoException(e);
        }
    }

}

class BuildComparator implements Comparator<BuildInfo> {
	public int compare(BuildInfo buildInfo1, BuildInfo buildInfo2) {
		DateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy hh:mm:ss");
		Date buildTime1 = new Date();
		Date buildTime2 = new Date();
		try {
			buildTime1 = (Date)formatter.parse(buildInfo1.getTimeStamp());
			buildTime2 = (Date)formatter.parse(buildInfo2.getTimeStamp());
		} catch (ParseException e) {
		}

		return buildTime2.compareTo(buildTime1);
	}
}