package com.photon.phresco.plugins.dotnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.Commandline;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.photon.phresco.commons.BuildInfo;
import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.plugin.commons.MavenProjectInfo;
import com.photon.phresco.plugin.commons.PluginConstants;
import com.photon.phresco.plugin.commons.PluginUtils;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration;
import com.photon.phresco.plugins.util.MojoUtil;
import com.photon.phresco.util.ArchiveUtil;
import com.photon.phresco.util.Utility;
import com.photon.phresco.util.ArchiveUtil.ArchiveType;

public class Package implements PluginConstants {
	
	private MavenProject project;
	private File baseDir;
	private String environmentName;
	private String buildName;
	private String buildNumber;
	private int buildNo;
	
	private File buildDir;
	private File targetDir;
	private File srcDir;
	private File buildInfoFile;
	private List<BuildInfo> buildInfoList;
	private int nextBuildNo;
	private String zipName;
	private Date currentDate;
	private Log log;
	
	public void pack(Configuration configuration, MavenProjectInfo mavenProjectInfo, Log log) throws PhrescoException {
		this.log = log;
		baseDir = mavenProjectInfo.getBaseDir();
		project = mavenProjectInfo.getProject();
        Map<String, String> configs = MojoUtil.getAllValues(configuration);
        environmentName = configs.get("environmentName");
        buildName = configs.get("buildName");
        
		log.info("Plugin Development is in Progress ...");
		try {
			init();
			boolean buildStatus = build();
			writeBuildInfo(buildStatus);
		} catch (MojoExecutionException e) {
			throw new PhrescoException(e);
		}
	}
	
	private void init() throws MojoExecutionException {
		try {
			
			buildDir = new File(baseDir.getPath() + PluginConstants.BUILD_DIRECTORY);
			buildInfoList = new ArrayList<BuildInfo>(); // initialization
			srcDir = new File(baseDir.getPath() + File.separator + "source/src");
			buildDir = new File(baseDir.getPath() + PluginConstants.BUILD_DIRECTORY);
			if (!buildDir.exists()) {
				buildDir.mkdirs();
				log.info("Build directory created..." + buildDir.getPath());
			}
			buildInfoFile = new File(buildDir.getPath() + PluginConstants.BUILD_INFO_FILE);
			targetDir = new File(project.getBuild().getDirectory());
			if (!targetDir.exists()) {
				targetDir.mkdirs();
				log.info("Target directory created..." + targetDir.getPath());
			}
			nextBuildNo = generateNextBuildNo();
			currentDate = Calendar.getInstance().getTime();
		} catch (Exception e) {
			log.error(e);
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
	
	private boolean build() throws MojoExecutionException {
		boolean isBuildSuccess = true;
		try {
			executeMsBuildCmd();
			executeAspCompilerCmd();
			createPackage();
		} catch (Exception e) {
			isBuildSuccess = false;
			log.error(e);
		}
		return isBuildSuccess;
	}

	private void executeMsBuildCmd() throws MojoExecutionException {
		BufferedReader in = null;
		try {
			Commandline cl = new Commandline("msbuild.exe SampleWebApp.csproj /t:rebuild");
			cl.setWorkingDirectory(baseDir.getPath() + "/source/src");
			Process process = cl.execute();
			in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
				System.out.println(line); // do not use log here as this line already contains the log type.
			}
		} catch (CommandLineException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			Utility.closeStream(in);
		}
	}
	private void executeAspCompilerCmd()throws MojoExecutionException {
		BufferedReader in = null;
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("aspnet_compiler -v / -p ");
			sb.append("\"" + srcDir.getPath() + "\"");
			sb.append(STR_SPACE);
			sb.append("-u");
			sb.append(STR_SPACE);
			sb.append("\"" + targetDir.getPath() + "\"");
			Commandline cl = new Commandline(sb.toString());
			cl.setWorkingDirectory(baseDir.getPath() + "/source/src");
			Process process = cl.execute();
			in = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
				System.out.println(line); // do not use log here as this line already contains the log type.
			}
		} catch (CommandLineException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			Utility.closeStream(in);
		}
	}
	private void createPackage() throws MojoExecutionException {
		try {
			if (buildName != null) {
				zipName = buildName + ".zip";
			} else {
				if (buildNumber != null) {
					zipName = PROJECT_CODE + buildNumber + STR_UNDERSCORE + getTimeStampForBuildName(currentDate)
							+ ".zip";
				} else {
					zipName = PROJECT_CODE + nextBuildNo + STR_UNDERSCORE + getTimeStampForBuildName(currentDate)
							+ ".zip";
				}
			}
			String zipFilePath = buildDir.getPath() + File.separator + zipName;
			ArchiveUtil.createArchive(targetDir.getPath(), zipFilePath, ArchiveType.ZIP);
		} catch (PhrescoException e) {
			throw new MojoExecutionException(e.getErrorMessage(), e);
		}
	}
	
	private void writeBuildInfo(boolean isBuildSuccess) throws MojoExecutionException {

		try {
			if (buildNumber != null) {
				buildNo = Integer.parseInt(buildNumber);
			}

			PluginUtils pu = new PluginUtils();
			BuildInfo buildInfo = new BuildInfo();
			List<String> envList = pu.csvToList(environmentName);
			if (buildNo > 0) {
				buildInfo.setBuildNo(buildNo);
			} else {
				buildInfo.setBuildNo(nextBuildNo);
			}
			buildInfo.setTimeStamp(getTimeStampForDisplay(currentDate));
			if (isBuildSuccess) {
				buildInfo.setBuildStatus(SUCCESS);
			} else {
				buildInfo.setBuildStatus(FAILURE);
			}
			buildInfo.setBuildName(zipName);
			buildInfo.setEnvironments(envList);
			buildInfoList.add(buildInfo);
			Gson gson = new Gson();
			FileWriter writer = new FileWriter(buildInfoFile);
			gson.toJson(buildInfoList, writer);
			writer.close();
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private String getTimeStampForDisplay(Date currentDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd/MMM/yyyy HH:mm:ss");
		String timeStamp = formatter.format(currentDate.getTime());
		return timeStamp;
	}

	private String getTimeStampForBuildName(Date currentDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MMM-yyyy-HH-mm-ss");
		String timeStamp = formatter.format(currentDate.getTime());
		return timeStamp;
	}

	private int generateNextBuildNo() throws IOException {
		int nextBuildNo = 1;
		if (!buildInfoFile.exists()) {
			return nextBuildNo;
		}

		BufferedReader read = new BufferedReader(new FileReader(buildInfoFile));
		String content = read.readLine();
		Gson gson = new Gson();
		Type listType = new TypeToken<List<BuildInfo>>() {
		}.getType();
		buildInfoList = (List<BuildInfo>) gson.fromJson(content, listType);
		if (buildInfoList == null || buildInfoList.size() == 0) {
			return nextBuildNo;
		}
		int buildArray[] = new int[buildInfoList.size()];
		int count = 0;
		for (BuildInfo buildInfo : buildInfoList) {
			buildArray[count] = buildInfo.getBuildNo();
			count++;
		}

		Arrays.sort(buildArray); // sort to the array to find the max build no

		nextBuildNo = buildArray[buildArray.length - 1] + 1; // increment 1 to the max in the build list
		return nextBuildNo;
	}

}