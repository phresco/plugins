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
package com.photon.phresco.plugins.cq5;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.gson.Gson;
import com.photon.phresco.commons.model.ApplicationInfo;
import com.photon.phresco.exception.ConfigurationException;
import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.framework.model.ContextUrls;
import com.photon.phresco.framework.model.PerformanceDetails;
import com.photon.phresco.plugin.commons.MavenProjectInfo;
import com.photon.phresco.plugin.commons.PluginConstants;
import com.photon.phresco.plugin.commons.PluginUtils;
import com.photon.phresco.plugins.api.ExecutionStatus;
import com.photon.phresco.plugins.impl.DefaultExecutionStatus;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration;
import com.photon.phresco.plugins.util.MojoUtil;
import com.photon.phresco.util.Constants;
import com.photon.phresco.util.Utility;
import com.phresco.pom.model.Plugin;
import com.phresco.pom.util.PomProcessor;

/**
 * @author jagadeesh_r
 * @goal load-test
 *
 */
public class LoadTest implements PluginConstants{

	public ExecutionStatus runLoadTest(Configuration configuration, MavenProjectInfo mavenProjectInfo) throws PhrescoException {
		try {
			PluginUtils pluginUtils = new PluginUtils();
			MavenProject project = mavenProjectInfo.getProject();
			String subModule = "";
			if (StringUtils.isNotEmpty(mavenProjectInfo.getModuleName())) {
				subModule = mavenProjectInfo.getModuleName();
			}
			File baseDir = project.getBasedir();
			File workingDir = project.getBasedir();
			if (StringUtils.isNotEmpty(subModule)) {
				workingDir = new File(workingDir + File.separator + subModule);
			}
			File dotPhrescoDir = baseDir;
			String pomXml = mavenProjectInfo.getProject().getFile().getName();
			File pomFile = new File(workingDir.getPath() + File.separatorChar + pomXml);
			PomProcessor processor = new PomProcessor(pomFile);
			String dotPhrescoDirName = processor.getProperty(Constants.POM_PROP_KEY_SPLIT_PHRESCO_DIR);
			if (StringUtils.isNotEmpty(dotPhrescoDirName)) {
				dotPhrescoDir = new File(baseDir.getParent() +  File.separatorChar + dotPhrescoDirName + File.separatorChar + subModule);
			}
			ApplicationInfo appInfo = pluginUtils.getAppInfo(dotPhrescoDir);
			String appDirName = appInfo.getAppDirName();
			String loadTestDirName = processor.getProperty(Constants.POM_PROP_KEY_SPLIT_TEST_DIR);
			String srcDirName = processor.getProperty(Constants.POM_PROP_KEY_SPLIT_SRC_DIR);
			File loadTestSplitDir = workingDir;
			if (StringUtils.isNotEmpty(loadTestDirName)) {
				loadTestSplitDir = new File(Utility.getProjectHome() + File.separatorChar + appDirName + File.separatorChar + loadTestDirName + File.separatorChar + subModule);
			} else if (StringUtils.isNotEmpty(srcDirName)) {
				loadTestSplitDir = new File(Utility.getProjectHome() + File.separatorChar + appDirName + File.separatorChar + srcDirName + File.separatorChar + subModule);
			}
 			Map<String, String> configs = MojoUtil.getAllValues(configuration);
			String testBasis = configs.get(TEST_BASIS);
			String customTestAgainst = configs.get(CUSTOM_TEST_AGAINST);
			String testAgainstType = configs.get(TEST_AGAINST);
			String testName = configs.get(KEY_TEST_NAME);
			String environmentName = configs.get(ENVIRONMENT_NAME);
			String type = configs.get(KEY_CONFIGURATION);
			String noOfUsers = configs.get(KEY_NO_OF_USERS);
			String rampUpPeriod = configs.get(KEY_RAMP_UP_PERIOD);
			String loopCount = configs.get(KEY_LOOP_COUNT);
			String jmxs = configs.get(AVAILABLE_JMX);
			String authManager = configs.get(KEY_AUTH_MANAGER);
			String authorizationUrl = configs.get(KEY_AUTHORIZATION_URL);
			String authorizationUserName = configs.get(KEY_AUTHORIZATION_USER_NAME);
			String authorizationPassword = configs.get(KEY_AUTHORIZATION_PASSWORD);
			String authorizationDomain = configs.get(KEY_AUTHORIZATION_DOMAIN);
			String authorizationRealm = configs.get(KEY_AUTHORIZATION_REALM);
			
			String loadTestAgainst = "";
			if (StringUtils.isNotEmpty(testBasis) && CUSTOMISE.equals(testBasis)) {
				loadTestAgainst = customTestAgainst;
			} else {
				loadTestAgainst = testAgainstType;
			}
			String loadTestDir = processor.getProperty(Constants.POM_PROP_KEY_LOADTEST_DIR);
			if(StringUtils.isNotEmpty(loadTestDir)) {
				loadTestDir = loadTestDir +  File.separator + loadTestAgainst;
				StringBuilder testPomPath = new StringBuilder(loadTestSplitDir.getPath())
				.append(loadTestDir)
				.append(File.separator)
				.append(POM_XML);
				File testPomFile = new File(testPomPath.toString());
				PomProcessor pomProcessor = new PomProcessor(testPomFile);
				Plugin plugin = pomProcessor.getPlugin(COM_LAZERYCODE_JMETER, JMETER_MAVEN_PLUGIN);
				
				DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
				DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
				Document doc = docBuilder.newDocument();
				com.phresco.pom.model.Plugin.Configuration jmeterConfiguration = plugin.getConfiguration();
				//If test basis is customise
				if (StringUtils.isNotEmpty(testBasis) && CUSTOMISE.equals(testBasis)) {
					List<String> jmxFiles = Arrays.asList(jmxs.split(CSV_PATTERN));
					if (testPomFile.exists()) {
						List<Element> configList = jmxUploadPluginConfiguration(doc, jmeterConfiguration, jmxFiles);
						pomProcessor.addConfiguration(COM_LAZERYCODE_JMETER, JMETER_MAVEN_PLUGIN, configList);
						pomProcessor.save();
					}
				} else {
					com.photon.phresco.configuration.Configuration config = null;
					List<com.photon.phresco.configuration.Configuration> configurations = pluginUtils.getConfiguration(dotPhrescoDir, environmentName, testAgainstType);
					for (com.photon.phresco.configuration.Configuration conf : configurations) {
						if (conf.getName().equals(type)) {
							config = conf;
							break;
						}
					}
					List<Element> configList = testAgainstParameterPluginConfiguration(doc, jmeterConfiguration, testName);
					pomProcessor.addConfiguration(COM_LAZERYCODE_JMETER, JMETER_MAVEN_PLUGIN, configList);
					pomProcessor.save();
					String testConfigFilePath = loadTestSplitDir.getPath() + File.separator + loadTestDir + File.separator + TESTS_FOLDER;
					pluginUtils.adaptTestConfig(testConfigFilePath + File.separator , config, "loadTest");
					String jsonFile = loadTestSplitDir.getPath() + File.separator + loadTestDir + File.separator + Constants.FOLDER_JSON + File.separator+ testName + Constants.DOT_JSON;
					BufferedReader bufferedReader = new BufferedReader(new FileReader(jsonFile));
					Gson gson = new Gson();
					PerformanceDetails fromJson = gson.fromJson(bufferedReader, PerformanceDetails.class);
					List<ContextUrls> contextUrls = fromJson.getContextUrls();

					pluginUtils.adaptLoadJmx(testConfigFilePath + File.separator, Integer.parseInt(noOfUsers), Integer.parseInt(rampUpPeriod), Integer.parseInt(loopCount), Boolean.parseBoolean(authManager),authorizationUrl, 
							authorizationUserName, authorizationPassword, authorizationDomain, authorizationRealm, contextUrls);
				}
			}
			generateMavenCommand(mavenProjectInfo, loadTestSplitDir.getPath() + File.separator + loadTestDir, LOAD);

		} catch (ConfigurationException e) {
			throw new PhrescoException(e);
		} catch (Exception e) {
			throw new PhrescoException(e);
		}
		return new DefaultExecutionStatus();
	}
	
	private List<Element> jmxUploadPluginConfiguration(Document doc, com.phresco.pom.model.Plugin.Configuration configuration, List<String> jmxFiles) {

		List<Element> configList = configuration.getAny();
		List<Element> newList = new ArrayList<Element>();
		for (Element element : configList) {
			if(TEST_FILES_DIRECTORY.equals(element.getTagName())) {
				element.setTextContent(jmxFiles.get(0).split(SEP)[0]);
				newList.add(element);
			} else if(!TEST_FILES_INCLUDED.equals(element.getTagName()) && !RESULT_FILES_NAME.equalsIgnoreCase(element.getTagName())) {
				newList.add(element);
			} 
		} 
		createResultFilesName(doc, jmxFiles, newList);
		createTestFilesIncludes(doc, jmxFiles, newList);

		return newList;
	}

	private void createTestFilesIncludes(Document doc, List<String> jmxFiles, List<Element> newList) {
		Element testFilesIncludedElement = doc.createElement(TEST_FILES_INCLUDED);
		for (String jmxFile : jmxFiles) {
			appendChildElement(doc, testFilesIncludedElement, JMETER_TEST_FILE, jmxFile.split(SEP)[1]);
		}
		newList.add(testFilesIncludedElement);
	}

	private void createResultFilesName(Document doc, List<String> jmxFiles, List<Element> newList) {
		Element resultFilesNameElement = doc.createElement(RESULT_FILES_NAME);
		for (String jmxFile : jmxFiles) {
			int lastDot = jmxFile.split(SEP)[1].lastIndexOf(DOT);
			String resultName = jmxFile.split(SEP)[1].substring(0, lastDot);
			appendChildElement(doc, resultFilesNameElement, RESULT_NAME, resultName);
		}
		newList.add(resultFilesNameElement);
	}

	private Element appendChildElement(Document doc, Element parent, String elementName, String textContent) {
		Element childElement = createElement(doc, elementName, textContent);
		parent.appendChild(childElement);
		return childElement;
	}

	private Element createElement(Document doc, String elementName, String textContent) {
		Element element = doc.createElement(elementName);
		if (StringUtils.isNotEmpty(textContent)) {
			element.setTextContent(textContent);
		}

		return element;
	}

	private List<Element> testAgainstParameterPluginConfiguration(Document doc, com.phresco.pom.model.Plugin.Configuration configuration, String resultName) {

		List<Element> configList = configuration.getAny();
		List<Element> newList = new ArrayList<Element>();
		for (Element element : configList) {
			if(TEST_FILES_DIRECTORY.equals(element.getTagName())) {
				element.setTextContent(TESTS_SLASH);
				newList.add(element);
			} else if(!TEST_FILES_INCLUDED.equals(element.getTagName()) && !RESULT_FILES_NAME.equalsIgnoreCase(element.getTagName())) {
				newList.add(element);
			} 
		} 

		Element resultFilesNameElement = doc.createElement(RESULT_FILES_NAME);
		appendChildElement(doc, resultFilesNameElement, RESULT_NAME, resultName);
		newList.add(resultFilesNameElement);

		Element testFilesIncludedElement = doc.createElement(TEST_FILES_INCLUDED);
		appendChildElement(doc, testFilesIncludedElement, JMETER_TEST_FILE, PHRESCO_FRAME_WORK_TEST_PLAN_JMX);
		newList.add(testFilesIncludedElement);

		return newList;
	}

	private void generateMavenCommand(MavenProjectInfo mavenProjectInfo, String workingDirectory, String actionType) throws PhrescoException {
		StringBuilder sb = new StringBuilder();
		File workingFile = new File(workingDirectory + File.separator + Constants.POM_NAME);
		sb.append(TEST_COMMAND);
		if(workingFile.exists()) {
			sb.append(STR_SPACE);
			sb.append(Constants.HYPHEN_F);
			sb.append(STR_SPACE);
			sb.append(Constants.POM_NAME);
		}
		File baseDir = mavenProjectInfo.getBaseDir();
		if (StringUtils.isNotEmpty(mavenProjectInfo.getModuleName())) {
			baseDir = new File(baseDir + File.separator + mavenProjectInfo.getModuleName());
		}
		boolean status = Utility.executeStreamconsumer(sb.toString(), workingDirectory, baseDir.getPath(), actionType);
		if(!status) {
			throw new PhrescoException(Constants.MOJO_ERROR_MESSAGE);
		}
	}
}
