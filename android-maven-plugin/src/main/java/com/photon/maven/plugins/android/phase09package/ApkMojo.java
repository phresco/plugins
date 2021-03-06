/**
 * Android Maven Plugin - android-maven-plugin
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

package com.photon.maven.plugins.android.phase09package;

import static com.photon.maven.plugins.android.common.AndroidExtension.APK;
import static com.photon.maven.plugins.android.common.AndroidExtension.APKLIB;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.AbstractScanner;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;

import com.photon.maven.plugins.android.AbstractAndroidMojo;
import com.photon.maven.plugins.android.AndroidSigner;
import com.photon.maven.plugins.android.CommandExecutor;
import com.photon.maven.plugins.android.ExecutionException;
import com.photon.maven.plugins.android.common.AetherHelper;
import com.photon.maven.plugins.android.common.EclipseAetherHelper;
import com.photon.maven.plugins.android.common.EclipseAetherNativeHelper;
import com.photon.maven.plugins.android.common.NativeHelper;
import com.photon.maven.plugins.android.configuration.Sign;
import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.plugin.commons.PluginConstants;
import com.photon.phresco.plugin.commons.PluginUtils;
import com.photon.phresco.util.Constants;
import com.photon.phresco.util.Utility;
import com.phresco.pom.exception.PhrescoPomException;
import com.phresco.pom.util.PomProcessor;


/**
 * Creates the apk file. By default signs it with debug keystore.<br/>
 * Change that by setting configuration parameter
 * <code>&lt;sign&gt;&lt;debug&gt;false&lt;/debug&gt;&lt;/sign&gt;</code>.
 *
 * @goal apk
 * @phase package
 * @requiresDependencyResolution compile
 */
public class ApkMojo extends AbstractAndroidMojo {

	/**
	 * <p>
	 * How to sign the apk.
	 * </p>
	 * <p>
	 * Looks like this:
	 * </p>
	 *
	 * <pre>
	 * &lt;sign&gt;
	 *     &lt;debug&gt;auto&lt;/debug&gt;
	 * &lt;/sign&gt;
	 * </pre>
	 * <p>
	 * Valid values for <code>&lt;debug&gt;</code> are:
	 * <ul>
	 * <li><code>true</code> = sign with the debug keystore.
	 * <li><code>false</code> = don't sign with the debug keystore.
	 * <li><code>both</code> = create a signed as well as an unsigned apk.
	 * <li><code>auto</code> (default) = sign with debug keystore, unless
	 * another keystore is defined. (Signing with other keystores is not yet
	 * implemented. See <a
	 * href="http://code.google.com/p/maven-android-plugin/issues/detail?id=2"
	 * >Issue 2</a>.)
	 * </ul>
	 * </p>
	 * <p>
	 * Can also be configured from command-line with parameter
	 * <code>-Dandroid.sign.debug</code>.
	 * </p>
	 *
	 * @parameter
	 */
	private Sign sign;

	/**
	 * <p>
	 * Parameter designed to pick up <code>-Dandroid.sign.debug</code> in case
	 * there is no pom with a <code>&lt;sign&gt;</code> configuration tag.
	 * </p>
	 * <p>
	 * Corresponds to
	 * {@link com.photon.maven.plugins.android.configuration.Sign#debug}.
	 * </p>
	 *
	 * @parameter expression="${android.sign.debug}" default-value="auto"
	 * @readonly
	 */
	private String signDebug;

	/**
	 * <p>
	 * A possibly new package name for the application. This value will be
	 * passed on to the aapt parameter --rename-manifest-package. Look to aapt
	 * for more help on this.
	 * </p>
	 *
	 * @parameter expression="${android.renameManifestPackage}"
	 */
	private String renameManifestPackage;

	/**
	 * <p>
	 * Rewrite the manifest so that all of its instrumentation components target
	 * the given package. This value will be passed on to the aapt parameter
	 * --rename-instrumentation-target-package. Look to aapt for more help on
	 * this.
	 * </p>
	 *
	 * @parameter expression="${android.renameInstrumentationTargetPackage}"
	 */
	private String renameInstrumentationTargetPackage;

	/**
	 * <p>
	 * Allows to detect and extract the duplicate files from embedded jars. In
	 * that case, the plugin analyzes the content of all embedded dependencies
	 * and checks they are no duplicates inside those dependencies. Indeed,
	 * Android does not support duplicates, and all dependencies are inlined in
	 * the APK. If duplicates files are found, the resource is kept in the first
	 * dependency and removes from others.
	 *
	 * @parameter expression="${android.extractDuplicates}"
	 *            default-value="false"
	 */
	private boolean extractDuplicates;

	/**
	 * <p>
	 * Temporary folder for collecting native libraries.
	 * </p>
	 *
	 * @parameter default-value="${project.build.directory}/libs"
	 * @readonly
	 */
	private File nativeLibrariesOutputDirectory;

	/**
	 * <p>
	 * Default hardware architecture for native library dependencies (with
	 * {@code &lt;type>so&lt;/type>}).
	 * </p>
	 * <p>
	 * This value is used for dependencies without classifier, if
	 * {@code nativeLibrariesDependenciesHardwareArchitectureOverride} is not
	 * set.
	 * </p>
	 * <p>
	 * Valid values currently include {@code armeabi} and {@code armeabi-v7a}.
	 * </p>
	 *
	 * @parameter expression=
	 *            "${android.nativeLibrariesDependenciesHardwareArchitectureDefault}"
	 *            default-value="armeabi"
	 */
	private String nativeLibrariesDependenciesHardwareArchitectureDefault;

	/**
	 * <p>
	 * Classifier to add to the artifact generated. If given, the artifact will
	 * be an attachment instead.
	 * </p>
	 *
	 * @parameter
	 */
	private String classifier;

	/**
	 * <p>
	 * Override hardware architecture for native library dependencies (with
	 * {@code &lt;type>so&lt;/type>}).
	 * </p>
	 * <p>
	 * This overrides any classifier on native library dependencies, and any
	 * {@code nativeLibrariesDependenciesHardwareArchitectureDefault}.
	 * </p>
	 * <p>
	 * Valid values currently include {@code armeabi} and {@code armeabi-v7a}.
	 * </p>
	 *
	 * @parameter expression=
	 *            "${android.nativeLibrariesDependenciesHardwareArchitectureOverride}"
	 */
	private String nativeLibrariesDependenciesHardwareArchitectureOverride;

	/**
	 * <p>
	 * Additional source directories that contain resources to be packaged into
	 * the apk.
	 * </p>
	 * <p>
	 * These are not source directories, that contain java classes to be
	 * compiled. It corresponds to the -df option of the apkbuilder program. It
	 * allows you to specify directories, that contain additional resources to
	 * be packaged into the apk.
	 * </p>
	 * So an example inside the plugin configuration could be:
	 *
	 * <pre>
	 * &lt;configuration&gt;
	 * 	  ...
	 *    &lt;sourceDirectories&gt;
	 *      &lt;sourceDirectory&gt;${project.basedir}/additionals&lt;/sourceDirectory&gt;
	 *   &lt;/sourceDirectories&gt;
	 *   ...
	 * &lt;/configuration&gt;
	 * </pre>
	 *
	 * @parameter expression="${android.sourceDirectories}" default-value=""
	 */
	private File[] sourceDirectories;

	/**
	 * @component
	 * @readonly
	 * @required
	 */
	protected ArtifactFactory artifactFactory;

	private static final Pattern PATTERN_JAR_EXT = Pattern.compile("^.+\\.jar$", 2);

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Make an early exit if we're not supposed to generate the APK
		if (!generateApk) {
			return;
		}
        configure();
        generateIntermediateAp_();
	    // Initialize apk build configuration
		File outputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + '.' + APK);
	
		final boolean signWithDebugKeyStore = getAndroidSigner().isSignWithDebugKeyStore();

		if (getAndroidSigner().shouldCreateBothSignedAndUnsignedApk()) {
			getLog().info("Creating debug key signed apk file " + outputFile);
			createApkFile(outputFile, true);
			final File unsignedOutputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + "-unsigned." + APK);
			getLog().info("Creating additional unsigned apk file " + unsignedOutputFile);
			createApkFile(unsignedOutputFile, false);
			projectHelper.attachArtifact(project, unsignedOutputFile, classifier == null ? "unsigned" : classifier + "_unsigned");
		} else {
			createApkFile(outputFile, signWithDebugKeyStore);
		}

		if (classifier == null) {
			// Set the generated .apk file as the main artifact (because the pom
			// states <packaging>apk</packaging>)
			project.getArtifact().setFile(outputFile);
		} else {
			// If there is a classifier specified, attach the artifact using that
			projectHelper.attachArtifact(project, outputFile, classifier);
		}
		
	}
  private void configure() throws MojoExecutionException {
			try {
				if (StringUtils.isEmpty(environmentName)||baseDir.getPath().endsWith("unit")
						|| baseDir.getPath().endsWith("functional")) {
				
				return;
				
			}
			getLog().info("Configuring the project....");
			
			PomProcessor pomProcessor = new PomProcessor(project.getFile());
			String configSourceDir = pomProcessor.getProperty(PluginConstants.POM_PROP_CONFIG_FILE);
			File srcConfigFile = null;
			if(StringUtils.isNotEmpty(configSourceDir)) {
				srcConfigFile  = new File(baseDir + configSourceDir);
			 } else {
				srcConfigFile = new File(sourceDirectory.getParent(), "/assets/phresco-env-config.xml");
			}
			File rootDir = new File(baseDir.getParentFile().getPath());
			File rootPomFile = new File(rootDir.getPath()+File.separator +"pom.xml");
			String dotPhrescoDirName=null;
			if(rootPomFile.exists()){
				
				PomProcessor rootPomProcessor = new PomProcessor(rootPomFile);
				dotPhrescoDirName = rootPomProcessor.getProperty(Constants.POM_PROP_KEY_SPLIT_PHRESCO_DIR);
			}
			
			if(dotPhrescoDirName!=null && !dotPhrescoDirName.isEmpty()){
				
				baseDir = new File(baseDir.getParentFile().getParentFile().getPath()+ File.separator +dotPhrescoDirName);
				
			}else{
				baseDir = new File(baseDir.getParentFile().getPath());
			}
			PluginUtils pu = new PluginUtils();
			pu.executeUtil(environmentName, baseDir.getPath() , srcConfigFile);
		    pu.setDefaultEnvironment(environmentName, srcConfigFile);
	     } catch (PhrescoException e) {
			throw new MojoExecutionException(e.getMessage()); 
		} catch (PhrescoPomException e) {
			throw new MojoExecutionException(e.getMessage());
		}
	}
    void createApkFile(File outputFile, boolean signWithDebugKeyStore) throws MojoExecutionException {

		File dexFile = new File(project.getBuild().getDirectory(), "classes.dex");
		File zipArchive = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_");
		ArrayList<File> sourceFolders = new ArrayList<File>();
		if (sourceDirectories != null) {
			for (File f : sourceDirectories) {
				sourceFolders.add(f);
			}
		}
		ArrayList<File> jarFiles = new ArrayList<File>();
		ArrayList<File> nativeFolders = new ArrayList<File>();

		boolean useInternalAPKBuilder = true;
		try {
			initializeAPKBuilder();
			// Ok...
			// So we can try to use the internal ApkBuilder
		} catch (Throwable e) {
			// Not supported platform try to old way.
			useInternalAPKBuilder = false;
		}

		// Process the native libraries, looking both in the current build
		// directory as well as
		// at the dependencies declared in the pom. Currently, all .so files are
		// automatically included
		processNativeLibraries(nativeFolders);

		if (useInternalAPKBuilder) {
			doAPKWithAPKBuilder(outputFile, dexFile, zipArchive, sourceFolders, jarFiles, nativeFolders, false, signWithDebugKeyStore, false);
		} else {
			doAPKWithCommand(outputFile, dexFile, zipArchive, sourceFolders, jarFiles, nativeFolders, signWithDebugKeyStore);
		}
	}

	private final Map<String, List<File>> m_jars = new HashMap<String, List<File>>();

	private void computeDuplicateFiles(File jar) throws  IOException {
		ZipFile file = new ZipFile(jar);
		Enumeration<? extends ZipEntry> list = file.entries();
		while (list.hasMoreElements()) {
			ZipEntry ze = list.nextElement();
			if (!(ze.getName().contains("META-INF/") || ze.isDirectory())) { // Exclude
																				// META-INF
																				// and
																				// Directories
				List<File> l = m_jars.get(ze.getName());
				if (l == null) {
					l = new ArrayList<File>();
					m_jars.put(ze.getName(), l);
				}
				l.add(jar);
			}
		}
	}

	/**
	 * Creates the APK file using the internal APKBuilder.
	 *
	 * @param outputFile
	 *            the output file
	 * @param dexFile
	 *            the dex file
	 * @param zipArchive
	 *            the classes folder
	 * @param sourceFolders
	 *            the resources
	 * @param jarFiles
	 *            the embedded java files
	 * @param nativeFolders
	 *            the native folders
	 * @param verbose
	 *            enables the verbose mode
	 * @param signWithDebugKeyStore
	 *            enables the signature of the APK using the debug key
	 * @param debug
	 *            enables the debug mode
	 * @throws MojoExecutionException
	 *             if the APK cannot be created.
	 */
	private void doAPKWithAPKBuilder(File outputFile, File dexFile, File zipArchive, ArrayList<File> sourceFolders, ArrayList<File> jarFiles, ArrayList<File> nativeFolders, boolean verbose,
			boolean signWithDebugKeyStore, boolean debug) throws MojoExecutionException {

		/* Following line doesn't make any difference if we keep it or comment it
		 * Commented By - Viral - Feb 11, 2012
		 */
//		sourceFolders.add(new File(project.getBuild().getDirectory(), "android-classes"));

		for (Artifact artifact : getRelevantCompileArtifacts()) {
			if (extractDuplicates) {
				try {
					computeDuplicateFiles(artifact.getFile());
				} catch (Exception e) {
					getLog().warn("Cannot compute duplicates files from " + artifact.getFile().getAbsolutePath(), e);
				}
			}
			jarFiles.add(artifact.getFile());
		}

		// Check duplicates.
		if (extractDuplicates) {
			List<String> duplicates = new ArrayList<String>();
			List<File> jarToModify = new ArrayList<File>();
			for (String s : m_jars.keySet()) {
				List<File> l = m_jars.get(s);
				if (l.size() > 1) {
					getLog().warn("Duplicate file " + s + " : " + l);
					duplicates.add(s);
					for (int i = 1; i < l.size(); i++) {
						if (!jarToModify.contains(l.get(i))) {
							jarToModify.add(l.get(i));
						}
					}
				}
			}

			// Rebuild jars.
			for (File file : jarToModify) {
				File newJar;
				newJar = removeDuplicatesFromJar(file, duplicates);
				int index = jarFiles.indexOf(file);
				if (newJar != null) {
					jarFiles.set(index, newJar);
				}

			}
		}

		ApkBuilder builder = new ApkBuilder(outputFile, zipArchive, dexFile, signWithDebugKeyStore, (verbose) ? System.out : null);

		if (debug) {
			builder.setDebugMode(debug);
		}
		/* Following code block is responsible to make the .apk size almost doubled.
		 * Commented By - Viral - Feb 11, 2012
		 */
//		for (File sourceFolder : sourceFolders) {
//			builder.addSourceFolder(sourceFolder);
//		}

		for (File jarFile : jarFiles) {
			if (jarFile.isDirectory()) {
				String[] filenames = jarFile.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return PATTERN_JAR_EXT.matcher(name).matches();
					}
				});

				for (String filename : filenames) {
					builder.addResourcesFromJar(new File(jarFile, filename));
				}
			} else {
				builder.addResourcesFromJar(jarFile);
			}
		}

		for (File nativeFolder : nativeFolders) {
			builder.addNativeLibraries(nativeFolder, null);
		}

		builder.sealApk();
	}

	private File removeDuplicatesFromJar(File in, List<String> duplicates) {
		File target = new File(project.getBasedir(), "target");
		File tmp = new File(target, "unpacked-embedded-jars");
		tmp.mkdirs();
		File out = new File(tmp, in.getName());

		if (out.exists()) {
			return out;
		}
		try {
			out.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Create a new Jar file
		FileOutputStream fos = null;
		ZipOutputStream jos = null;
		try {
			fos = new FileOutputStream(out);
			jos = new ZipOutputStream(fos);
		} catch (FileNotFoundException e1) {
			getLog().error("Cannot remove duplicates : the output file " + out.getAbsolutePath() + " does not found");
			return null;
		}

		ZipFile inZip = null;
		try {
			inZip = new ZipFile(in);
			Enumeration<? extends ZipEntry> entries = inZip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				// If the entry is not a duplicate, copy.
				if (!duplicates.contains(entry.getName())) {
					// copy the entry header to jos
					jos.putNextEntry(entry);
					InputStream currIn = inZip.getInputStream(entry);
					copyStreamWithoutClosing(currIn, jos);
					currIn.close();
					jos.closeEntry();
				}
			}
		} catch (IOException e) {
			getLog().error("Cannot removing duplicates : " + e.getMessage());
			return null;
		}

		try {
			if (inZip != null) {
				inZip.close();
			}
			jos.close();
			fos.close();
			jos = null;
			fos = null;
		} catch (IOException e) {
			// ignore it.
		}
		getLog().info(in.getName() + " rewritten without duplicates : " + out.getAbsolutePath());
		return out;
	}

	/**
	 * Copies an input stream into an output stream but does not close the
	 * streams.
	 *
	 * @param in
	 *            the input stream
	 * @param out
	 *            the output stream
	 * @throws IOException
	 *             if the stream cannot be copied
	 */
	private static void copyStreamWithoutClosing(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[4096];
		for (int n; (n = in.read(b)) != -1;) {
			out.write(b, 0, n);
		}
	}

	/**
	 * Creates the APK file using the command line.
	 *
	 * @param outputFile
	 *            the output file
	 * @param dexFile
	 *            the dex file
	 * @param zipArchive
	 *            the classes folder
	 * @param sourceFolders
	 *            the resources
	 * @param jarFiles
	 *            the embedded java files
	 * @param nativeFolders
	 *            the native folders
	 * @param signWithDebugKeyStore
	 *            enables the signature of the APK using the debug key
	 * @throws MojoExecutionException
	 *             if the APK cannot be created.
	 */
	private void doAPKWithCommand(File outputFile, File dexFile, File zipArchive, ArrayList<File> sourceFolders, ArrayList<File> jarFiles, ArrayList<File> nativeFolders, boolean signWithDebugKeyStore)
			throws MojoExecutionException {

		CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
		executor.setLogger(this.getLog());

		List<String> commands = new ArrayList<String>();
		commands.add(outputFile.getAbsolutePath());

		if (!signWithDebugKeyStore) {
			commands.add("-u");
		}

		commands.add("-z");
		commands.add(new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_").getAbsolutePath());
		commands.add("-f");
		commands.add(new File(project.getBuild().getDirectory(), "classes.dex").getAbsolutePath());
		commands.add("-rf");
		commands.add(new File(project.getBuild().getDirectory(), "classes").getAbsolutePath());

		if (nativeFolders != null && !nativeFolders.isEmpty()) {
			for (File lib : nativeFolders) {
				commands.add("-nf");
				commands.add(lib.getAbsolutePath());
			}
		}

		for (Artifact artifact : getRelevantCompileArtifacts()) {
			commands.add("-rj");
			commands.add(artifact.getFile().getAbsolutePath());
		}

		getLog().info( getAndroidSdk().getApkBuilderPath() + " " + commands.toString() );
		try {
			executor.executeCommand( getAndroidSdk().getApkBuilderPath(), commands, project.getBasedir(),false );
		} catch (ExecutionException e) {
			throw new MojoExecutionException("", e);
		}
	}

	private void initializeAPKBuilder() throws MojoExecutionException {
		File file = getAndroidSdk().getSDKLibJar();
		ApkBuilder.initialize(getLog(), file);
	}

	private void processNativeLibraries(final List<File> natives) throws MojoExecutionException {
		// Examine the native libraries directory for content. This will only be
		// true if:
		// a) the directory exists
		// b) it contains at least 1 file
		final boolean hasValidNativeLibrariesDirectory = nativeLibrariesDirectory != null && nativeLibrariesDirectory.exists()
				&& (nativeLibrariesDirectory.listFiles() != null && nativeLibrariesDirectory.listFiles().length > 0);

		// Retrieve any native dependencies or attached artifacts. This may
		// include artifacts from the ndk-build MOJO
		Set<Artifact> artifacts = null;
		if (container.hasComponent("org.sonatype.aether.RepositorySystem")) {
    		org.sonatype.aether.RepositorySystem system;
			try {
				system = container.lookup(org.sonatype.aether.RepositorySystem.class);
			} catch (ComponentLookupException e) {
				throw new MojoExecutionException(e.getMessage());
			}
    		org.sonatype.aether.RepositorySystemSession repositorySession = session.getRepositorySession();
    		List<org.sonatype.aether.repository.RemoteRepository> remoteProjectRepositories = project.getRemoteProjectRepositories();
    		NativeHelper nativeHelper = new NativeHelper(project, remoteProjectRepositories, repositorySession, system, artifactFactory, getLog());
    		artifacts = nativeHelper.getNativeDependenciesArtifacts(unpackedApkLibsDirectory, true);
    	} else if (container.hasComponent("org.eclipse.aether.RepositorySystem")) {
    		org.eclipse.aether.RepositorySystem system;
			try {
				system = container.lookup(org.eclipse.aether.RepositorySystem.class);
			} catch (ComponentLookupException e) {
				throw new MojoExecutionException(e.getMessage());
			}
			DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
	        LocalRepository localRepo = new LocalRepository( Utility.getLocalRepoPath());
	        session.setLocalRepositoryManager( system.newLocalRepositoryManager( session, localRepo ) );
	        List<?> repositories = project.getRemoteProjectRepositories();
	        EclipseAetherNativeHelper nativeHelper = new EclipseAetherNativeHelper(project, repositories, 
	        		session, system, artifactFactory, getLog());
	        artifacts = nativeHelper.getNativeDependenciesArtifacts(unpackedApkLibsDirectory, true);
    	}

		final boolean hasValidBuildNativeLibrariesDirectory = nativeLibrariesOutputDirectory.exists()
				&& (nativeLibrariesOutputDirectory.listFiles() != null && nativeLibrariesOutputDirectory.listFiles().length > 0);

		if (artifacts.isEmpty() && hasValidNativeLibrariesDirectory && !hasValidBuildNativeLibrariesDirectory) {

			getLog().debug("No native library dependencies detected, will point directly to " + nativeLibrariesDirectory);

			// Point directly to the directory in this case - no need to copy
			// files around
			natives.add(nativeLibrariesDirectory);
		} else if (!artifacts.isEmpty() || hasValidNativeLibrariesDirectory) {
			// In this case, we may have both .so files in it's normal location
			// as well as .so dependencies

			// Create the ${project.build.outputDirectory}/libs
			final File destinationDirectory = new File(nativeLibrariesOutputDirectory.getAbsolutePath());
			destinationDirectory.mkdirs();

			// Point directly to the directory
			natives.add(destinationDirectory);

			// If we have a valid native libs, copy those files - these already
			// come in the structure required
			if (hasValidNativeLibrariesDirectory) {
				copyLocalNativeLibraries(nativeLibrariesDirectory, destinationDirectory);
			}

			if (!artifacts.isEmpty()) {
				for (Artifact resolvedArtifact : artifacts) {
					if ("so".equals(resolvedArtifact.getType())) {
						final File artifactFile = resolvedArtifact.getFile();
						try {
							final String artifactId = resolvedArtifact.getArtifactId();
							final String filename = artifactId.startsWith("lib") ? artifactId + ".so" : "lib" + artifactId + ".so";

							final File finalDestinationDirectory = getFinalDestinationDirectoryFor(resolvedArtifact, destinationDirectory);
							final File file = new File(finalDestinationDirectory, filename);
							getLog().debug("Copying native dependency " + artifactId + " (" + resolvedArtifact.getGroupId() + ") to " + file);
							org.apache.commons.io.FileUtils.copyFile(artifactFile, file);
						} catch (Exception e) {
							throw new MojoExecutionException("Could not copy native dependency.", e);
						}
					} else if (APKLIB.equals(resolvedArtifact.getType())) {
						natives.add(new File(getLibraryUnpackDirectory(resolvedArtifact) + "/libs"));
					}
				}
			}
		}
	}

	private File getFinalDestinationDirectoryFor(Artifact resolvedArtifact, File destinationDirectory) {
		final String hardwareArchitecture = getHardwareArchitectureFor(resolvedArtifact);

		File finalDestinationDirectory = new File(destinationDirectory, hardwareArchitecture + "/");

		finalDestinationDirectory.mkdirs();
		return finalDestinationDirectory;
	}

	private String getHardwareArchitectureFor(Artifact resolvedArtifact) {

		if (StringUtils.isNotBlank(nativeLibrariesDependenciesHardwareArchitectureOverride)) {
			return nativeLibrariesDependenciesHardwareArchitectureOverride;
		}

		final String classifier = resolvedArtifact.getClassifier();
		if (StringUtils.isNotBlank(classifier)) {
			return classifier;
		}

		return nativeLibrariesDependenciesHardwareArchitectureDefault;
	}

	private void copyLocalNativeLibraries(final File localNativeLibrariesDirectory, final File destinationDirectory) throws MojoExecutionException {
		getLog().debug("Copying existing native libraries from " + localNativeLibrariesDirectory);
		try {
			org.apache.commons.io.FileUtils.copyDirectory(localNativeLibrariesDirectory, destinationDirectory, new FileFilter() {
				@Override
				public boolean accept(final File pathname) {
					return pathname.getName().endsWith(".so");
				}
			});
		} catch (IOException e) {
			getLog().error("Could not copy native libraries: " + e.getMessage(), e);
			throw new MojoExecutionException("Could not copy native dependency.", e);
		}
	}

	/**
	 * Generates an intermediate apk file (actually .ap_) containing the
	 * resources and assets.
	 *
	 * @throws MojoExecutionException
	 */
	private void generateIntermediateAp_() throws MojoExecutionException {
		CommandExecutor executor = CommandExecutor.Factory.createDefaultCommmandExecutor();
		executor.setLogger(this.getLog());
		File[] overlayDirectories;

		if (resourceOverlayDirectories == null || resourceOverlayDirectories.length == 0) {
			overlayDirectories = new File[] { resourceOverlayDirectory };
		} else {
			overlayDirectories = resourceOverlayDirectories;
		}

		if (extractedDependenciesRes.exists()) {
			try {
				getLog().info("Copying dependency resource files to combined resource directory.");
				if (!combinedRes.exists() && !combinedRes.mkdirs()) {
					throw new MojoExecutionException("Could not create directory for combined resources at " + combinedRes.getAbsolutePath());
				}
				org.apache.commons.io.FileUtils.copyDirectory(extractedDependenciesRes, combinedRes);
			} catch (IOException e) {
				throw new MojoExecutionException("", e);
			}
		}
		if (resourceDirectory.exists() && combinedRes.exists()) {
			try {
				getLog().info("Copying local resource files to combined resource directory.");
				org.apache.commons.io.FileUtils.copyDirectory(resourceDirectory, combinedRes, new FileFilter() {

					/**
					 * Excludes files matching one of the common file to
					 * exclude. The default excludes pattern are the ones from
					 * {org
					 * .codehaus.plexus.util.AbstractScanner#DEFAULTEXCLUDES}
					 *
					 * @see java.io.FileFilter#accept(java.io.File)
					 */
					@Override
					public boolean accept(File file) {
						for (String pattern : AbstractScanner.DEFAULTEXCLUDES) {
							if (AbstractScanner.match(pattern, file.getAbsolutePath())) {
								getLog().debug("Excluding " + file.getName() + " from resource copy : matching " + pattern);
								return false;
							}
						}
						return true;
					}
				});
			} catch (IOException e) {
				throw new MojoExecutionException("", e);
			}
		}

		// Must combine assets.
		// The aapt tools does not support several -A arguments.
		// We copy the assets from extracted dependencies first, and then the
		// local assets.
		// This allows redefining the assets in the current project
		if (extractedDependenciesAssets.exists()) {
			try {
				getLog().info("Copying dependency assets files to combined assets directory.");
				org.apache.commons.io.FileUtils.copyDirectory(extractedDependenciesAssets, combinedAssets, new FileFilter() {
					/**
					 * Excludes files matching one of the common file to
					 * exclude. The default excludes pattern are the ones from
					 * {org
					 * .codehaus.plexus.util.AbstractScanner#DEFAULTEXCLUDES}
					 *
					 * @see java.io.FileFilter#accept(java.io.File)
					 */
					@Override
					public boolean accept(File file) {
						for (String pattern : AbstractScanner.DEFAULTEXCLUDES) {
							if (AbstractScanner.match(pattern, file.getAbsolutePath())) {
								getLog().debug("Excluding " + file.getName() + " from asset copy : matching " + pattern);
								return false;
							}
						}

						return true;

					}
				});
			} catch (IOException e) {
				throw new MojoExecutionException("", e);
			}
		}

		// Next pull APK Lib assets, reverse the order to give precedence to
		// libs higher up the chain
		List<Artifact> artifactList = new ArrayList<Artifact>(getAllRelevantDependencyArtifacts());
		for (Artifact artifact : artifactList) {
			if (artifact.getType().equals(APKLIB)) {
				File apklibAsssetsDirectory = new File(getLibraryUnpackDirectory(artifact) + "/assets");
				if (apklibAsssetsDirectory.exists()) {
					try {
						getLog().info("Copying dependency assets files to combined assets directory.");
						org.apache.commons.io.FileUtils.copyDirectory(apklibAsssetsDirectory, combinedAssets, new FileFilter() {
							/**
							 * Excludes files matching one of the common file to
							 * exclude. The default excludes pattern are the
							 * ones from
							 * {org.codehaus.plexus.util.AbstractScanner
							 * #DEFAULTEXCLUDES}
							 *
							 * @see java.io.FileFilter#accept(java.io.File)
							 */
							@Override
							public boolean accept(File file) {
								for (String pattern : AbstractScanner.DEFAULTEXCLUDES) {
									if (AbstractScanner.match(pattern, file.getAbsolutePath())) {
										getLog().debug("Excluding " + file.getName() + " from asset copy : matching " + pattern);
										return false;
									}
								}

								return true;

							}
						});
					} catch (IOException e) {
						throw new MojoExecutionException("", e);
					}

				}
			}
		}

		if (assetsDirectory.exists()) {
			try {
				getLog().info("Copying local assets files to combined assets directory.");
				org.apache.commons.io.FileUtils.copyDirectory(assetsDirectory, combinedAssets, new FileFilter() {
					/**
					 * Excludes files matching one of the common file to
					 * exclude. The default excludes pattern are the ones from
					 * {org
					 * .codehaus.plexus.util.AbstractScanner#DEFAULTEXCLUDES}
					 *
					 * @see java.io.FileFilter#accept(java.io.File)
					 */
					@Override
					public boolean accept(File file) {
						for (String pattern : AbstractScanner.DEFAULTEXCLUDES) {
							if (AbstractScanner.match(pattern, file.getAbsolutePath())) {
								getLog().debug("Excluding " + file.getName() + " from asset copy : matching " + pattern);
								return false;
							}
						}

						return true;

					}
				});
			} catch (IOException e) {
				throw new MojoExecutionException("", e);
			}
		}

		File androidJar = getAndroidSdk().getAndroidJar();
		File outputFile = new File(project.getBuild().getDirectory(), project.getBuild().getFinalName() + ".ap_");

		List<String> commands = new ArrayList<String>();
		commands.add("package");
		commands.add("-f");
		commands.add("-M");
		commands.add(androidManifestFile.getAbsolutePath());
		for (File resOverlayDir : overlayDirectories) {
			if (resOverlayDir != null && resOverlayDir.exists()) {
				commands.add("-S");
				commands.add(resOverlayDir.getAbsolutePath());
			}
		}
		if (combinedRes.exists()) {
			commands.add("-S");
			commands.add(combinedRes.getAbsolutePath());
		} else {
			if (resourceDirectory.exists()) {
				commands.add("-S");
				commands.add(resourceDirectory.getAbsolutePath());
			}
		}
		for (Artifact artifact : getAllRelevantDependencyArtifacts()) {
			if (artifact.getType().equals(APKLIB)) {
				final String apkLibResDir = getLibraryUnpackDirectory(artifact) + "/res";
				if (new File(apkLibResDir).exists()) {
					commands.add("-S");
					commands.add(apkLibResDir);
				}
			}
		}
		commands.add("--auto-add-overlay");

		// Use the combined assets.
		// Indeed, aapt does not support several -A arguments.
		if (combinedAssets.exists()) {
			commands.add("-A");
			commands.add(combinedAssets.getAbsolutePath());
		}

		if (StringUtils.isNotBlank(renameManifestPackage)) {
			commands.add("--rename-manifest-package");
			commands.add(renameManifestPackage);
		}

		if (StringUtils.isNotBlank(renameInstrumentationTargetPackage)) {
			commands.add("--rename-instrumentation-target-package");
			commands.add(renameInstrumentationTargetPackage);
		}

		commands.add("-I");
		commands.add(androidJar.getAbsolutePath());
		commands.add("-F");
		commands.add(outputFile.getAbsolutePath());
		if (StringUtils.isNotBlank(configurations)) {
			commands.add("-c");
			commands.add(configurations);
		}

		for (String aaptExtraArg : aaptExtraArgs) {
			commands.add(aaptExtraArg);
		}

		getLog().info( getAndroidSdk().getAaptPath() + " " + commands.toString() );
		try {
			executor.executeCommand( getAndroidSdk().getAaptPath(), commands, project.getBasedir(), false );
		} catch (ExecutionException e) {
			throw new MojoExecutionException("", e);
		}
	}

	protected AndroidSigner getAndroidSigner() {
		if (sign == null) {
			return new AndroidSigner(signDebug);
		}
		return new AndroidSigner(sign.getDebug());
	}
}
