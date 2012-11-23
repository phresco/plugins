package com.photon.phresco.plugins.php;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.jettison.json.JSONException;

import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.plugin.commons.MavenProjectInfo;
import com.photon.phresco.plugins.PhrescoBasePlugin;
import com.photon.phresco.plugins.model.Mojos.Mojo.Configuration;

public class PHPPlugin extends PhrescoBasePlugin {
   
	public PHPPlugin(Log log) {
		super(log);
	}
	
	@Override
    public void pack(Configuration configuration, MavenProjectInfo mavenProjectInfo) throws PhrescoException {
    	Package pack = new Package();
        pack.pack(configuration, mavenProjectInfo, getLog());
    }
	
	@Override
    public void deploy(Configuration configuration, MavenProjectInfo mavenProjectInfo) throws PhrescoException {
        Deploy deploy = new Deploy();
        try {
			deploy.deploy(configuration, mavenProjectInfo, getLog());
		} catch (JSONException e) {
			throw new PhrescoException(e);
		}
    }
}
