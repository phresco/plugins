/**
 * Xcodebuild Command-Line Wrapper
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

import java.io.*;
import java.util.*;

import org.apache.commons.lang.*;
import org.apache.maven.plugin.*;
import org.apache.maven.project.*;

import com.photon.phresco.commons.model.*;
import com.photon.phresco.plugin.commons.*;
import com.photon.phresco.plugins.xcode.utils.*;
import com.photon.phresco.plugins.xcode.utils.XcodeUtil;
/**
 * APP deploy
 * @goal deploy
 * @phase deploy
 */
public class AppDeploy extends AbstractMojo implements PluginConstants {


	/**
	 * command line tool for iphone simulator
	 * @parameter expression="${simulator.home}" default-value="/Developer/Platforms/iPhoneSimulator.platform/Developer/Applications/iPhone Simulator.app/Contents/MacOS/iPhone Simulator"
	 */
	private String simHome;
	
	/**
	 * 
	 * @parameter experssion="${simulator.deploy.dir}" default-value=""
	 */
	private String appDeployHome;

	/**
	 * @parameter expression ="${action}" default-value="-SimulateApplication"
	 * 
	 * The possible actions are SimulateDevice, SimulateRestart, SessionOnLaunch, currentSDKRoot
	 */
	private String action;
	
	/**
	 * @parameter expression="${simulator.version}" default-value="5.0" 
	 */
	private String simVersion;
	
	/**
	 * @parameter expression="${family}" default-value="iphone" 
	 */
	private String family;
	
	/**
	 * @parameter expression="${device.deploy}"
	 */
	private boolean deviceDeploy;
	
	/**
	 * @parameter expression="${application.name}"
	 */
	private String appName;
	
	/**
	 * The maven project.
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
	
	/**
	 * @parameter expression="${buildNumber}" required="true"
	 */
	protected String buildNumber;
	
	/**
	 * @parameter expression ="${triggerSimulator}" default-value="true"
	 */
	private boolean triggerSimulator;
	
	private String appPath;
	
	private File simHomeAppLocation;
	
	private String home = "";
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			if(!deviceDeploy && !SdkVerifier.isAvailable(simVersion)) {
				throw new MojoExecutionException("Selected version " +simVersion +" is not available!");
			}
		} catch (IOException e2) {
			throw new MojoExecutionException("SDK verification failed!");
		} catch (InterruptedException e2) {
			throw new MojoExecutionException("SDK verification interrupted!");
		}
		//get the correct simhome if xCode 4.3 is installed simhome is in /Application/Xcode.app/Contents
		//Fix for artf462004
		File simHomeFile = new File(simHome);
		if(!simHomeFile.exists()) {
			simHome = "/Applications/Xcode.app/Contents" + simHome; 
		}
		
		home = System.getProperty("user.home");
		// Have to get app path from build id
		if (StringUtils.isEmpty(buildNumber)) {
			throw new MojoExecutionException("Selected build is not available!");
		}
		getLog().info("Build Number : " + buildNumber);
		getLog().info("Project Code : " + baseDir.getName());
		
		PluginUtils pu = new PluginUtils();
		BuildInfo buildInfo = pu.getBuildInfo(Integer.parseInt(buildNumber));
		appPath = buildInfo.getDeployLocation();
		
		if(!deviceDeploy && triggerSimulator) {
			// Deploying to Simulator
			getLog().info("Deploying App to Simulator");
			deployAppToSimulator();
		} else if (!deviceDeploy && !triggerSimulator) { 
			// jenkins executes it.
			// This process copies the app file and places it in simulator's applications directory.
			getLog().info("Copying App To Simulator");
			copyAppToSimHome();
		}
		Runnable runnable = new Runnable() {
			public void run() {
				ProcessBuilder pb;
				if (deviceDeploy) {
					pb = new ProcessBuilder("transporter_chief.rb");
					pb.command().add(appPath);
				} else {
					pb = new ProcessBuilder(simHome);
					// Include errors in output
					pb.redirectErrorStream(true);

					pb.command().add(action);
					pb.command().add(simHomeAppLocation+File.separator+appName.substring(0, appName.indexOf('.')));
				}

				getLog().info("List of Commands : " + pb.command());
				Process child;
				try {
//					if(ProcessHelper.isProcessRunning()) {
//						ProcessHelper.killSimulatorProcess();
//					}
					child = pb.start();
					// Consume subprocess output and write to stdout for debugging
					InputStream is = new BufferedInputStream(child.getInputStream());
					int singleByte = 0;
					while ((singleByte = is.read()) != -1) {
						System.out.write(singleByte);
					}
				} catch (IOException e) {
					getLog().error("error occured in launching simulator ");
					getLog().error(e);
				}
			}
		};

		// For device deployment alone
		if (deviceDeploy) { // Phresco app deploy should deploy the app in both simulator and device, In jenkins, app should start running only on device deployment - Due to Pipeline issue
			Thread t = new Thread(runnable, "iPhoneSimulator");
			t.start();
			try {
				t.join(30000);
			} catch (InterruptedException e1) {
				getLog().error("Triggering simulator failed.");
			}
		}

	}

	private void deployAppToSimulator() {
		try {
			/*String waxsim_home = System.getenv(WAXSIM_HOME);
			if(StringUtils.isEmpty(waxsim_home)) {
				throw new MojoExecutionException("waxsim_home is not found!");
			}*/
			Runnable deployRunnable = new Runnable() {
				public void run() {
					List<String> command = new ArrayList<String>();
					/*command.add(System.getenv(WAXSIM_HOME));
					command.add("-s");
					command.add(simVersion);
					command.add("-f");
					command.add(family);
					command.add(appPath);*/
					command.add(IOS_SIM);
					command.add(LAUNCH);
					command.add(appPath);
					command.add(HYPHEN_SDK);
					command.add(simVersion);
					command.add(HYPHEN_FAMILY);
					command.add(family);
					getLog().info("Command : " + command.toString());

					try {
						ProcessBuilder pb = new ProcessBuilder(command);
						pb.directory(baseDir);
						Process child = pb.start();
						InputStream is = new BufferedInputStream(child.getInputStream());
						int singleByte = 0;
						while ((singleByte = is.read()) != -1) {
							System.out.write(singleByte);
						}
					} catch (IOException e) {
						getLog().error("error occured in launching simulator ");
						getLog().error(e);
					}
				}
			};

			Thread t = new Thread(deployRunnable, "iPhoneSimulator");
			t.start();
			try {
				t.join(30000);
			} catch (InterruptedException e1) {
				getLog().error("Triggering simulator failed.");
			}
			
		} catch (Exception e) {
			getLog().error("Error occured in deploying to simulator");
			getLog().error(e);
		}
	}

	private void copyAppToSimHome() throws MojoExecutionException {	
		getLog().info("Application.path = " + appPath);
		appName = getAppFileName(appPath);
		getLog().info("Application name = "+ appName);
		String deployHome = "";
		if(StringUtils.isNotBlank(appDeployHome)){
			deployHome = appDeployHome;
		} else {
			deployHome = home + "/Library/Application Support/iPhone Simulator/" + simVersion +"/Applications";
		}
		simHomeAppLocation = new File(deployHome + File.separator + project.getName() + File.separator + appName);
		if(!simHomeAppLocation.exists()){
			simHomeAppLocation.mkdirs();
		}
		getLog().info("Simulator home Desired location "+ simHomeAppLocation.getAbsolutePath());
		try {
			String alignedPath = alignedPath(appPath);
			getLog().info("path to copy source :" + alignedPath);
			XcodeUtil.copyFolder(new File(alignedPath), simHomeAppLocation);
			getLog().info("copy the application " + appPath +" to "+ simHomeAppLocation);
		} catch (IOException e1) {
			getLog().error("couldn't copy the application " + appPath +" to "+ simHomeAppLocation);
		}
	}

	private String getAppFileName(String appPath2) {
		if(StringUtils.isNotBlank(appPath2)){
			if(appPath2.endsWith("app")) {
				return appPath2.substring(appPath2.lastIndexOf('/')+1);
			} else {
				File folder = new File(appPath2);
				if(folder.exists()) {
					File[] files = folder.listFiles();
					for (int i = 0; i < files.length; i++) {
						File file = files[i];
						if(file.getName().endsWith("app")) {
							return file.getName();
						}
					}
				}
			}
		}
		return appPath2;
		
	}
	
	private String alignedPath(String appPath2) {
		if(StringUtils.isNotBlank(appPath2)){
			if(appPath2.endsWith("app")) {
				return appPath2;
			}	else {
				return appPath2+File.separator+getAppFileName(appPath2);
			}
		}
		return appPath2;
	}
	
}