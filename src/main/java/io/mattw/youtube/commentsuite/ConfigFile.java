package io.mattw.youtube.commentsuite;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Saves and loads a data object to the specified JSON file.
 * <p>
 * Can take any object as long as it has fields that Gson is configured to read.
 *
 * @param <T> data object JSON (de)serialized
 */
public class ConfigFile<T> {

    private static final Logger logger = LogManager.getLogger();

    private final Gson gson = new Gson();

    private final File file;
    private final T defaultObject;
    private T dataObject;

    public ConfigFile(final String fileName, final T defaultObject) {
        logger.debug("Initialize ConfigFile<{}> [fileName={}]", defaultObject.getClass().getSimpleName(), fileName);
        this.defaultObject = defaultObject;
        this.dataObject = defaultObject;
        this.file = new File(fileName);
        if (!file.exists()) {
            save();
        } else {
            load();
        }
    }

    public void load() {
        logger.debug("Loading Config File");
        try (final FileReader fr = new FileReader(file);
             final BufferedReader br = new BufferedReader(fr)) {
            String line;
            StringBuilder data = new StringBuilder();
            while ((line = br.readLine()) != null) {
                data.append(line);
            }
            this.dataObject = (T) gson.fromJson(data.toString(), defaultObject.getClass());
            if (this.dataObject == null) {
                logger.debug("Parsed config file returned null. Using default config data.");
                this.dataObject = defaultObject;
            }
        } catch (Exception e) {
            logger.error(e);
            logger.debug("Using default config data on error loading file.");
            this.dataObject = defaultObject;
        }
    }

    public void save() {
        logger.debug("Saving Config File");
        try (final FileOutputStream fos = new FileOutputStream(file);
             final OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {
            writer.write(gson.toJson(dataObject));
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public T getDataObject() {
        return dataObject;
    }

    public void setDataObject(T dataObject) {
        this.dataObject = dataObject;
    }
}
