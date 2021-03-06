/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.pms.configuration;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import net.pms.PMS;
import net.pms.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author mfranco
 */
public class MapFileConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(MapFileConfiguration.class);
	private static final PmsConfiguration configuration = PMS.getConfiguration();
	private String name;
	private String thumbnailIcon;
	private List<MapFileConfiguration> children;
	private List<File> files;

	public String getName() {
		return name;
	}

	public String getThumbnailIcon() {
		return thumbnailIcon;
	}

	public List<MapFileConfiguration> getChildren() {
		return children;
	}

	public List<File> getFiles() {
		return files;
	}

	public void setName(String n) {
		name = n;
	}

	public void setThumbnailIcon(String t) {
		thumbnailIcon = t;
	}

	public void setFiles(List<File> f) {
		files = f;
	}

	public MapFileConfiguration() {
		children = new ArrayList<>();
		files = new ArrayList<>();
	}

	@Deprecated
	public static List<MapFileConfiguration> parse(String conf) {
		return parseVirtualFolders(null);
	}

	public static List<MapFileConfiguration> parseVirtualFolders(ArrayList<String> tags) {
		String conf;

		if (configuration.getVirtualFoldersFile(tags).trim().length() > 0) {
			// Get the virtual folder info from the user's file
			conf = configuration.getVirtualFoldersFile(tags).trim().replaceAll("&comma;", ",");
			File file = new File(configuration.getProfileDirectory(), conf);
			conf = null;

			if (FileUtil.isFileReadable(file)) {
				try {
					conf = FileUtils.readFileToString(file);
				} catch (IOException ex) {
					return null;
				}
			} else {
				LOGGER.warn("Can't read file: {}", file.getAbsolutePath());
			}

			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(File.class, new FileSerializer());
			Gson gson = gsonBuilder.create();
			Type listType = (new TypeToken<ArrayList<MapFileConfiguration>>() { }).getType();
			List<MapFileConfiguration> out = gson.fromJson(conf, listType);
			return out;
		} else if (configuration.getVirtualFolders(tags).trim().length() > 0) {
			// Get the virtual folder info from the config string
			conf = configuration.getVirtualFolders(tags).trim().replaceAll("&comma;", ",");
			String jsonStringFromConf = "";

			// Convert our syntax into JSON syntax
			String arrayLevel1[] = conf.split("\\|");
			int i = 0;
			boolean firstLoop = true;
			for (String value : arrayLevel1) {
				if (!firstLoop) {
					jsonStringFromConf += ",";
				}

				if (i == 0) {
					jsonStringFromConf += "[{\"name\":\"" + value + "\",files:[";
					i++;
				} else {
					String arrayLevel2[] = value.split(",");
					for (String value2 : arrayLevel2) {
						jsonStringFromConf += "\"" + value2 + "\",";
					}

					jsonStringFromConf += "]}]";
					firstLoop = false;
					i = 0;
				}
			}

			GsonBuilder gsonBuilder = new GsonBuilder();
			gsonBuilder.registerTypeAdapter(File.class, new FileSerializer());
			Gson gson = gsonBuilder.create();
			Type listType = (new TypeToken<ArrayList<MapFileConfiguration>>() { }).getType();
			List<MapFileConfiguration> out = gson.fromJson(jsonStringFromConf.replaceAll("\\\\","\\\\\\\\"), listType);

			return out;
		}

		return null;
	}
}

class FileSerializer implements JsonSerializer<File>, JsonDeserializer<File> {
	private static final Logger LOGGER = LoggerFactory.getLogger(FileSerializer.class);

	@Override
	public JsonElement serialize(File src, Type typeOfSrc, JsonSerializationContext context) {
		return new JsonPrimitive(src.getAbsolutePath());
	}

	@Override
	public File deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		File file = new File(json.getAsJsonPrimitive().getAsString());

		if (!FileUtil.isDirectoryReadable(file)) {
			LOGGER.warn("Can't read directory: {}", file.getAbsolutePath());
			return null;
		} else {
			return file;
		}
	}
}
