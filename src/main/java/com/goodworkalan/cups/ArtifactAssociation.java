package com.goodworkalan.cups;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import com.goodworkalan.go.go.library.Artifact;
import com.goodworkalan.go.go.version.VersionSelector;
import com.goodworkalan.madlib.VariableProperties;

/**
 * Associates artifacts read from configuration files with a string value.
 * Used to associate artifacts with the Mix commands used to install them. 
 * 
 * @author Alan Gutierrez
 */
public class ArtifactAssociation {
    /** The map of group and name parts to a map of strings indexed by version. */
	private final Map<List<String>, Map<String, String>> commands;
	
	/** The pattern to match an unversioned artifact. */
	private final static Pattern UNVERSIONED_ARTIFACT = Pattern.compile("[^/]+/[^/]+");
	/** The pattern to match a versioned artifact. */
	private final static Pattern VERSIONED_ARTIFACT = Pattern.compile("[^/]+/[^/]+/[^/]+");

    /**
     * Create an artifact association that reads the properties file resources
     * obtained from the class loader at the given path.
     * 
     * @param path
     *            The path.
     * @throws IOException
     *             For any I/O error.
     */
	public ArtifactAssociation(String path) throws IOException {
		this.commands = getCommands(path);
	}

	/**
	 * Read the commands from the version to command map files in the classpath.
	 * .
	 * 
	 * @return A map of unversioned artifact keys to a map of versions to
	 *         commands.
	 * @throws IOException
	 *             For any I/O error.
	 */
	public static Map<List<String>, Map<String, String>> getCommands(String path) throws IOException {
		Map<List<String>, Map<String, String>> commands = new HashMap<List<String>, Map<String, String>>();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> urls = classLoader.getResources(path);
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			Properties properties = new Properties();
			properties.load(url.openStream());
			VariableProperties variables = new VariableProperties(properties);
			for (Object key : properties.keySet()) {
				String name = key.toString();
				boolean unversioned = UNVERSIONED_ARTIFACT.matcher(name).matches();
				boolean versioned = false;
				if (!unversioned) {
					versioned = VERSIONED_ARTIFACT.matcher(name).matches();
				}
				if (unversioned || versioned) {
					Artifact artifact = new Artifact(name);
					Map<String, String> versions = commands.get(artifact.getUnversionedKey());
					if (versions == null) {
						versions = new HashMap<String, String>();
						commands.put(artifact.getUnversionedKey(), versions);
					}
					String value = variables.getProperty(name);
					String version = versioned ? artifact.getVersion() : "*";
					versions.put(version, value);
				}
			}
		}
		return commands;
	}

    /**
     * Get the string associated with the given artifact or <code>null</code> if
     * there is no string associated with the artifact.
     * 
     * @param artifact
     *            The artifact.
     * @return The string associated with the artifact or <code>null</code>.
     */
	public String get(Artifact artifact) {
		Map<String, String> versions = commands.get(artifact.getUnversionedKey());
		if (versions == null) {
			versions = Collections.emptyMap();
		}
		VersionSelector selector = new VersionSelector(artifact.getVersion());
		String version = selector.select(versions.keySet());
		if (version == null && versions.containsKey("*")) {
			version = "*";
		}
		return version == null ? null : versions.get(version);
	}
}
