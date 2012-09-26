package com.photon.phresco.plugins;

import java.lang.reflect.Constructor;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;

import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.plugins.api.PhrescoPlugin;

public abstract class PhrescoAbstractMojo extends AbstractMojo {
    
	public static final String PHRESCO_PLUGIN_INFO_XML = ".phresco/phresco-plugin-info.xml";
    
    public PhrescoPlugin getPlugin(String pluginClassName) throws PhrescoException {
        //Caching not needed since it will be triggered as a new process every time from the maven
    	return constructClass(pluginClassName);
    }

    private PhrescoPlugin constructClass(String pluginClassName) throws PhrescoException {
        try {
            Log log = getLog();
            Class<PhrescoPlugin> pluginClass = (Class<PhrescoPlugin>) Class.forName(pluginClassName);
            Constructor<PhrescoPlugin> constructor = pluginClass.getDeclaredConstructor(Log.class);
            return constructor.newInstance(log);
        } catch (Exception e) {
            throw new PhrescoException(e);
        }
    }
}
