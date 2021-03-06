/**
 * Phresco Maven Plugin
 *
 * Copyright (C) 1999-2013 Photon Infotech Inc.
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

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.photon.phresco.api.DynamicParameter;
import com.photon.phresco.commons.model.ApplicationInfo;
import com.photon.phresco.commons.model.ArtifactGroup;
import com.photon.phresco.commons.model.ArtifactInfo;
import com.photon.phresco.commons.model.ProjectInfo;
import com.photon.phresco.exception.ConfigurationException;
import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.framework.PhrescoFrameworkFactory;
import com.photon.phresco.plugin.commons.MavenProjectInfo;
import com.photon.phresco.plugin.commons.PluginConstants;
import com.photon.phresco.plugin.commons.PluginUtils;
import com.photon.phresco.plugins.api.PhrescoPlugin;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter.Name.Value;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter.PossibleValues;
import com.photon.phresco.plugins.model.Mojos.Mojo.Implementation;
import com.photon.phresco.plugins.model.Mojos.Mojo.Implementation.Dependency;
import com.photon.phresco.plugins.util.MavenEclipseAetherResolver;
import com.photon.phresco.plugins.util.MavenPluginArtifactResolver;
import com.photon.phresco.plugins.util.MavenSonatypeAetherResolver;
import com.photon.phresco.plugins.util.MojoProcessor;
import com.photon.phresco.service.client.api.ServiceContext;
import com.photon.phresco.service.client.api.ServiceManager;
import com.photon.phresco.service.client.impl.ServiceManagerImpl;
import com.photon.phresco.util.Constants;
import com.photon.phresco.util.Utility;

public abstract class PhrescoAbstractMojo extends AbstractMojo {

	
	/**
	 * @parameter expression="${project.basedir}" required="true"
	 * @readonly
	 */
	protected File baseDir;
	
	/**
     * @parameter expression="${package.version}"
     * @readonly
     */
    protected String buildVersion;
	
	//Value
	String paramval="false";
	protected Properties serverProperties;
	protected Properties projectProperties;
	private File serverFile;
	private ServiceManager serviceManager;
	List<String> excludedependency = new ArrayList<String>();
	private MavenSession mavenSession;
	private MavenProject mavenProject;
	private PlexusContainer container;
	
	private Map<String, Boolean> depMap = new HashMap<String, Boolean>();
	
//	public PhrescoPlugin getPlugin(Dependency dependency) throws PhrescoException {
//		//Caching not needed since it will be triggered as a new process every time from the maven
//		return constructClass(dependency);
//	}
	
	public PhrescoPlugin getPlugin(Dependency dependency, MavenSession mavenSession, MavenProject mavenProject, PlexusContainer container) throws PhrescoException {
		//Caching not needed since it will be triggered as a new process every time from the maven
		this.mavenSession = mavenSession;
		this.mavenProject = mavenProject;
		this.container = container;
		return constructClass(dependency);
	}

	private PhrescoPlugin constructClass(Dependency dependency) throws PhrescoException {
		Log log = getLog();
		try {
			Class<PhrescoPlugin> apiClass = (Class<PhrescoPlugin>) Class
			.forName(dependency.getClazz(), true, getURLClassLoader(dependency));
			Constructor<PhrescoPlugin> constructor = apiClass.getDeclaredConstructor(Log.class);
			return constructor.newInstance(log);
		} catch (Exception e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		}
	}

	private URLClassLoader getURLClassLoader(Dependency dependency) throws PhrescoException {
		List<Artifact> artifacts = new ArrayList<Artifact>();
		List<org.sonatype.aether.artifact.Artifact> artifacts2 = new ArrayList<org.sonatype.aether.artifact.Artifact>();
		List<ArtifactGroup> plugins  = new ArrayList<ArtifactGroup>();
		ArtifactGroup artifactGroup = new ArtifactGroup();
		artifactGroup.setGroupId(dependency.getGroupId());
		artifactGroup.setArtifactId(dependency.getArtifactId());
		artifactGroup.setPackaging(dependency.getType());
		ArtifactInfo info = new ArtifactInfo();
		info.setVersion(dependency.getVersion());
		artifactGroup.setVersions(Collections.singletonList(info));
		plugins.add(artifactGroup);

		for (ArtifactGroup plugin : plugins) {
			List<ArtifactInfo> versions = plugin.getVersions();
			for (ArtifactInfo artifactInfo : versions) {
				Artifact artifact = new DefaultArtifact(plugin.getGroupId(), plugin.getArtifactId(), PluginConstants.PACKAGING_TYPE_JAR, artifactInfo.getVersion());
				artifacts.add(artifact);
			}
		}
		URL[] artifactURLs = null;
				try {
					if (container.hasComponent("org.sonatype.aether.RepositorySystem")) {
						for (ArtifactGroup plugin : plugins) {
							List<ArtifactInfo> versions = plugin.getVersions();
							for (ArtifactInfo artifactInfo : versions) {
								org.sonatype.aether.artifact.Artifact artifact = new org.sonatype.aether.util.artifact.DefaultArtifact(plugin.getGroupId(), plugin.getArtifactId(), PluginConstants.PACKAGING_TYPE_JAR, artifactInfo.getVersion());
								artifacts2.add(artifact);
							}
						}
					org.sonatype.aether.RepositorySystem system = container.lookup(org.sonatype.aether.RepositorySystem.class);
					org.sonatype.aether.RepositorySystemSession session = mavenSession.getRepositorySession();
					List<org.sonatype.aether.repository.RemoteRepository> repositories = mavenProject.getRemoteProjectRepositories();
					artifactURLs = MavenSonatypeAetherResolver.resolve(repositories.get(0), artifacts2, system, session);
				} else if (container.hasComponent("org.eclipse.aether.RepositorySystem")) {
					org.eclipse.aether.RepositorySystem system = container.lookup(org.eclipse.aether.RepositorySystem.class);
					DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
			        LocalRepository localRepo = new LocalRepository( Utility.getLocalRepoPath());
			        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );
			        List<?> repositories = mavenProject.getRemoteProjectRepositories();
					artifactURLs = MavenEclipseAetherResolver.resolve(repositories.get(0), artifacts, system, session);
				}
				} catch (ComponentLookupException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				} catch (NoSuchMethodException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
		ClassLoader clsLoader = Thread.currentThread().getContextClassLoader();
		if (clsLoader == null) {
			clsLoader = this.getClass().getClassLoader();
		}
		URLClassLoader classLoader = new URLClassLoader(artifactURLs, clsLoader);
		return classLoader;
	}

	protected Configuration getConfiguration(String infoFile, String goal) throws PhrescoException {
		MojoProcessor processor = new MojoProcessor(new File(infoFile));
		return processor.getConfiguration(goal);
	}

	protected MavenProjectInfo getMavenProjectInfo(MavenProject project) {
		MavenProjectInfo mavenProjectInfo = new MavenProjectInfo();
		mavenProjectInfo.setBaseDir(project.getBasedir());
		mavenProjectInfo.setProject(project);
		mavenProjectInfo.setProjectCode(project.getBasedir().getName());
		return mavenProjectInfo;
	}
	
	protected MavenProjectInfo getMavenProjectInfo(MavenProject project, String subModule) {
        MavenProjectInfo mavenProjectInfo = new MavenProjectInfo();
    	mavenProjectInfo.setBaseDir(project.getBasedir());
        mavenProjectInfo.setProject(project);
        mavenProjectInfo.setProjectCode(project.getBasedir().getName());
        mavenProjectInfo.setModuleName(subModule);
        return mavenProjectInfo;
    }
	
	protected MavenProjectInfo getMavenProjectInfo(MavenProject project, String subModule, Map<String, Object> keyValues) {
		MavenProjectInfo mavenProjectInfo = getMavenProjectInfo(project, subModule);
//		keyValues.put(PluginConstants.REMOTE_REPOS, projectRepos);
//    	keyValues.put(PluginConstants.REPO_SYSTEM, repoSystem);
//    	keyValues.put(PluginConstants.REPO_SESSION, repoSession);
		mavenProjectInfo.setKeyValues(keyValues);
		return mavenProjectInfo;
	}
	
	protected MavenProjectInfo getMavenProjectInfo(MavenProject project, String subModule, MavenSession mavenSession, 
			BuildPluginManager pluginManager,ArtifactRepository localRepository) {
        MavenProjectInfo mavenProjectInfo = new MavenProjectInfo();
    	mavenProjectInfo.setBaseDir(project.getBasedir());
        mavenProjectInfo.setProject(project);
        mavenProjectInfo.setProjectCode(project.getBasedir().getName());
        mavenProjectInfo.setModuleName(subModule);
        mavenProjectInfo.setPluginManager(pluginManager);
        mavenProjectInfo.setMavenSession(mavenSession);
        mavenProjectInfo.setLocalRepository(localRepository);
        mavenProjectInfo.setBuildVersion(buildVersion);
        return mavenProjectInfo;
    }
	
	protected Dependency getDependency(String infoFile, String goal) throws PhrescoException {
		MojoProcessor processor = new MojoProcessor(new File(infoFile));
		if (processor.getImplementationDependency(goal) != null) {
			return processor.getImplementationDependency(goal).getDependency().get(0);
		}
		return null;
	}
	
	protected Dependency getDependency(String infoFile, String goal, String dependencyId) throws PhrescoException {
        MojoProcessor processor = new MojoProcessor(new File(infoFile));
        Implementation implementation = processor.getImplementationDependency(goal);
        if (implementation != null) {
            List<Dependency> listDependency = implementation.getDependency();
            for (Dependency dependency : listDependency) {
                if (dependencyId != null && dependencyId.equals(dependency.getId())) {
                    return dependency;
                }
            }
            return listDependency.get(0);
        }
        return null;
    }

	protected boolean isGoalAvailable(String infoFile, String goal) throws PhrescoException {
		MojoProcessor processor = new MojoProcessor(new File(infoFile));
		return processor.isGoalAvailable(goal);
	}
	
	private void setDependentMap(List<Parameter> parameters) {
		List<String> arrayList = new ArrayList<String>();
		PluginUtils pu = new PluginUtils();
		for (Parameter parameter : parameters) {
			if (StringUtils.isNotEmpty(parameter.getDependency())) {
				String dependencies = parameter.getDependency();
				List<String> depList = pu.csvToList(dependencies);
				for (String dependency : depList) {
					depMap.put(parameter.getKey() + dependency, true);
				}
			}
			if(parameter.getType().equalsIgnoreCase("List")) {
				List<com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter.PossibleValues.Value> possibleValues = parameter.getPossibleValues().getValue();
				setDependentMap(arrayList, pu, possibleValues);
			} if (!arrayList.contains(parameter.getKey())) {
				depMap.put(parameter.getKey(), true);
			}
		}
	}

	private void setDependentMap(List<String> arrayList, PluginUtils pu,
			List<com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter.PossibleValues.Value> possibleValues) {
		List<String> dependencyList = null;
		for (int i = 0; i < possibleValues.size(); i++) {
			String key = possibleValues.get(i).getKey();
			if (StringUtils.isNotEmpty(possibleValues.get(i).getDependency())) {
				dependencyList = pu.csvToList(possibleValues.get(i).getDependency());
				for (String dependency : dependencyList) {
					depMap.put(key + dependency, true);
					arrayList.add(dependency);
				}
			} if (CollectionUtils.isNotEmpty(dependencyList)) {
				for (String dependency : dependencyList) {
					if (!depMap.containsKey(key + dependency)) {
						depMap.put(key + dependency, false);
						arrayList.add(dependency);
					}
				}
			}
		}
	}
	
	protected void initProperty(String servicePropertyFile, String projectPropertyFile) throws MojoExecutionException {
		try {
			serverProperties = new Properties();
			projectProperties = new Properties();
			if(StringUtils.isEmpty(servicePropertyFile)) {
				String servicePropertyPath = getValueFromConfigFile("server.properties");
				serverFile = new File(servicePropertyPath);
			} else {
				serverFile = new File(servicePropertyFile);
				if(!serverFile.exists()) {
					serverFile = new File(baseDir, servicePropertyFile);
					if(!serverFile.exists()) {
						throw new MojoExecutionException("Server Property File Not Exists");
					}
				}
			}
			File projectPropFile = new File(projectPropertyFile);
			if(StringUtils.isEmpty(servicePropertyFile)) {
				String projectPropertyPath = getValueFromConfigFile("createproject.properties");
				projectPropFile = new File(projectPropertyPath);
			} else {
				if(!projectPropFile.exists()) {
					projectPropFile = new File(baseDir, projectPropertyFile);
					if(!projectPropFile.exists()) {
						throw new MojoExecutionException("Project Property File Not Exists");
					}
				}
			}
			projectProperties.load(new FileInputStream(projectPropFile));
			serverProperties.load(new FileInputStream(serverFile));
			if(StringUtils.isNotEmpty(servicePropertyFile) && StringUtils.isNotEmpty(projectPropertyFile)) {
				saveConfigFiles(serverFile.getAbsolutePath(), projectPropFile.getAbsolutePath());
			}
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException(e.getMessage());
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}
	
	private void saveConfigFiles(String serverPropPath, String createProjectPath) throws MojoExecutionException {
		String binDir = com.photon.phresco.util.Utility.getPhrescoHome() + "/bin";
		File configFile = new File(binDir, "configfiles.properties");
		Properties configProperties = new Properties();
		try {
			configProperties.put("server.properties", serverPropPath);
			configProperties.put("createproject.properties", createProjectPath);
			configProperties.save(new FileOutputStream(configFile), "");
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException(e.getMessage());
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}
	
	private String getValueFromConfigFile(String key) throws MojoExecutionException {
		String value = "";
		String binDir = com.photon.phresco.util.Utility.getPhrescoHome() + "/bin";
		File configFile = new File(binDir, "configfiles.properties");
		Properties configProperties = new Properties();
		try {
			configProperties.load(new FileInputStream(configFile));
			value = configProperties.getProperty(key);
		} catch (FileNotFoundException e) {
			throw new MojoExecutionException(e.getMessage());
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		return value;
	}
	
	protected ServiceManager getServiceManager() throws PhrescoException,
		MojoExecutionException, FileNotFoundException, IOException {
		String serverUrl = (String) serverProperties.get("phresco.service.url");
		String authToken = (String) serverProperties.get("auth.token");
		serviceManager = new ServiceManagerImpl(serverUrl, authToken);
		boolean validToken = serviceManager.isValidToken();
		if (!validToken) {
			System.out.println("Enter Username : ");
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			String userName = br.readLine();
			System.out.println("Enter Password : ");
			Console cons = System.console();
			char[] pwd = cons.readPassword();
			String password = new String(pwd);
			serviceManager = PhrescoFrameworkFactory.getServiceManager(getServiceContext(userName, password));
			authToken = serviceManager.getUserInfo().getToken();
			serverProperties.setProperty("auth.token", authToken);
			FileOutputStream out = new FileOutputStream(serverFile);
			serverProperties.store(out, "");
		}
		return serviceManager;
	}

	private ServiceContext getServiceContext(String userName, String password) throws MojoExecutionException {
		ServiceContext serviceContext;
		serviceContext = new ServiceContext();
		serviceContext.put("phresco.service.url", serverProperties.get("phresco.service.url"));
		serviceContext.put("phresco.service.username", userName);
		serverProperties.put("phresco.service.username", userName);
		serviceContext.put("phresco.service.password", password);
		return serviceContext;
	}
	
	protected Configuration getInteractiveConfiguration(Configuration configuration, MojoProcessor processor, MavenProject project, String goal) throws PhrescoException {
		try {
		if (configuration == null) {
			return null;
		}
		initProperty("", "");
		String serverUrl = (String) serverProperties.get("phresco.service.url");
		String authToken = (String) serverProperties.get("auth.token");
		serviceManager = new ServiceManagerImpl(serverUrl, authToken);
		boolean validToken = serviceManager.isValidToken();
		if (!validToken) {
			System.out.println("Session Expired  Enter Your Password : ");
			//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			Console cons = System.console();
			char[] pwd = cons.readPassword();
			String password = new String(pwd);
			String userName = (String) serverProperties.get("phresco.service.username");
			serviceManager = new ServiceManagerImpl(getServiceContext(userName, password));
		}
		Parameters parameters = configuration.getParameters();
		List<Parameter> parameter = parameters.getParameter();
		setDependentMap(parameter);
			List<String> valueList = new ArrayList<String>();
			for (Parameter param : parameter) {
				if(param.getType().equals("Hidden")) {
					continue;
				}
				String value = "";
				boolean show = false;
				if (CollectionUtils.isNotEmpty(valueList)) {
					for (String value1 : valueList) {
						if (depMap.containsKey(value1 + param.getKey())) {
							show = depMap.get(value1 + param.getKey());
						} 
					}
				} if (depMap.containsKey(param.getKey())) {
					show = depMap.get(param.getKey());
				}
				if (show) {
					Value name = param.getName().getValue().get(0);
					value = getValue(param, name, project, processor, goal);
					if(!value.isEmpty()) {
						valueList.add(value);
						param.setValue(value);
					}
				}
			}
			processor.save();
		} catch (MojoExecutionException e) {
			throw new PhrescoException(e);
		}
		return configuration;
	}

	private String getValue(Parameter parameter, Value value, MavenProject project, MojoProcessor processor, String goal) throws MojoExecutionException {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String paramValue = "";
		
		try {
			if(parameter.getType().equalsIgnoreCase("Boolean") & ! parameter.getKey().equalsIgnoreCase("showSettings")) {
				if(value.getValue().equalsIgnoreCase("ZipAlign") || value.getValue().equalsIgnoreCase("Signing")){
					String Dependencyvalue=parameter.getDependency();
					if(StringUtils.isNotEmpty(Dependencyvalue))	{
						String[] Dependencyvalues=Dependencyvalue.split(",");
						for(String depvalue:Dependencyvalues)	{
							excludedependency.add(depvalue.toLowerCase());
						}
					}
					return "";
				}
				System.out.println("Enter Value For " + value.getValue() + " (Y/N)");
				String readValue = br.readLine();
				paramValue = String.valueOf(true);
				paramval="true";
				if(readValue.equalsIgnoreCase("N")) {
					paramval="false";
					paramValue = String.valueOf(false);
					String Dependencyvalue=parameter.getDependency();
					if(StringUtils.isNotEmpty(Dependencyvalue))	{
						String[] Dependencyvalues=Dependencyvalue.split(",");
						for(String depvalue:Dependencyvalues)	{
							excludedependency.add(depvalue.toLowerCase());
						}
					}
				
				}
			}

			if(parameter.getType().equalsIgnoreCase("List") & !excludedependency.contains(value.getValue().toLowerCase())) {
				Map<String, String> pMap = new HashMap<String, String>();
				Map<String, com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter.PossibleValues.Value> possibleValMap = new HashMap<String, com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter.PossibleValues.Value>();
				List<com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.
				Parameter.PossibleValues.Value> possibleValues = parameter.getPossibleValues().getValue();
				System.out.println("Enter value for " + value.getValue());
				for (int i = 0; i < possibleValues.size(); i++) {
					System.out.println(i + "." + possibleValues.get(i).getValue());
					pMap.put(String.valueOf(i), possibleValues.get(i).getKey());
					possibleValMap.put(String.valueOf(i), possibleValues.get(i));
				}
				String enteredValue = br.readLine();
				paramValue = pMap.get(enteredValue);
			}
			
			if(parameter.getType().equalsIgnoreCase("DynamicParameter") & !excludedependency.contains(value.getValue().toLowerCase())) {
				if((value.getValue().equals("DataBase")||value.getValue().equals("FetchSql"))
						&& ( paramval.equalsIgnoreCase("false"))) {
					return "";
				}
				paramValue = getEnvironmentName(parameter, value, project, processor, goal);
			}

			if(parameter.getType().equalsIgnoreCase("Number") & !excludedependency.contains(value.getValue().toLowerCase())) {
				System.out.println("Enter value for " + value.getValue());
				paramValue = br.readLine();
			}
			if(parameter.getType().equalsIgnoreCase("String") & !excludedependency.contains(value.getValue().toLowerCase()) ) {
				System.out.println("Enter value for " + value.getValue());
				paramValue = br.readLine();
			}
			if(parameter.getType().equalsIgnoreCase("FileBrowse") & !excludedependency.contains(value.getValue().toLowerCase()) ) {
				System.out.println("Enter File Path for " + value.getValue());
				paramValue = br.readLine();
			}
			if(parameter.getType().equalsIgnoreCase("Hidden") & !excludedependency.contains(value.getValue().toLowerCase())) {
				System.out.println("Enter value for " + value.getValue());
				paramValue = br.readLine();
			}
			if(parameter.getType().equalsIgnoreCase("map") & !excludedependency.contains(value.getValue().toLowerCase())) {
			    paramValue = "";
			}
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		return paramValue;
	}

	private String getEnvironmentName(Parameter parameter, Value value, MavenProject project, MojoProcessor processor, String goal) throws MojoExecutionException {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			Map<String, String> envMap = new HashMap<String, String>(); 
			System.out.println("Select " + parameter.getName().getValue().get(0).getValue());
			PossibleValues possibleValue = dynamicClassLoader(parameter, project, processor, goal);
			List<com.photon.phresco.plugins.model.Mojos.Mojo.Configuration.Parameters.Parameter.PossibleValues.Value> values = possibleValue.getValue();

			for (int i = 0; i < values.size(); i++) {
				System.out.println(i + "." + values.get(i).getValue());
				envMap.put(String.valueOf(i), values.get(i).getValue());
			} 
			return envMap.get(br.readLine());
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} catch (PhrescoException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private PossibleValues dynamicClassLoader(Parameter parameter, MavenProject project, MojoProcessor processor, String goal) throws PhrescoException {
		try {
			File projectInfoFile = new File(project.getBasedir().getPath() + File.separatorChar + Constants.DOT_PHRESCO_FOLDER + File.separatorChar + Constants.PROJECT_INFO_FILE);
			Gson gson = new Gson();
			ProjectInfo projectInfo = gson.fromJson(new FileReader(projectInfoFile), ProjectInfo.class);
			ApplicationInfo applicationInfo = projectInfo.getAppInfos().get(0);
			String customerId = projectInfo.getCustomerIds().get(0);
			String clazz = parameter.getDynamicParameter().getClazz();
			Class loadClass = getClassFromLocal(clazz);
			Parameter buildNoParameter = processor.getParameter(goal, DynamicParameter.KEY_BUILD_NO);
			String buildNo = "";
			if (buildNoParameter != null) {
				buildNo = buildNoParameter.getValue();
			}
			if(loadClass != null ) {				
				DynamicParameter dynamicParameter = (DynamicParameter) loadClass.newInstance();
				Map<String, Object> dynamicParameterMap = new HashMap<String, Object>();
				dynamicParameterMap.put(DynamicParameter.KEY_APP_INFO, applicationInfo);
				dynamicParameterMap.put(DynamicParameter.KEY_CUSTOMER_ID, customerId);
				dynamicParameterMap.put(DynamicParameter.KEY_MOJO, processor);
				dynamicParameterMap.put(DynamicParameter.KEY_GOAL, goal);
				dynamicParameterMap.put(DynamicParameter.KEY_MULTI_MODULE, false);
				dynamicParameterMap.put(DynamicParameter.KEY_BUILD_NO, buildNo);
				dynamicParameterMap.put(DynamicParameter.REQ_SERVICE_MANAGER, serviceManager);
				return dynamicParameter.getValues(dynamicParameterMap);
			}
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		} catch (PhrescoException e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		} catch (InstantiationException e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		} catch (IOException e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		} catch (SAXException e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		} catch (ConfigurationException e) {
			e.printStackTrace();
			throw new PhrescoException(e);
		}
		return null;
	}

	private Class getClassFromLocal(String className) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		try {
			return classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		// TODO Auto-generated method stub
		
	}
}
