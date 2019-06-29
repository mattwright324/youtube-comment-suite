package io.mattw.youtube.commentsuite;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Saves and loads a data object to the specified JSON file.
 *
 * Can take any object as long as it has fields that Gson is configured to read.
 *
 * @author mattwright324
 * @param <T> data object JSON (de)serialized
 */
public class ConfigFile<T> {

    private static Logger logger = LogManager.getLogger(ConfigFile.class.getSimpleName());

    private Gson gson = new Gson();

    private T defaultObject;
    private T dataObject;
    private File file;

    public ConfigFile(String fileName, T defaultObject) {
        logger.debug("Initialize ConfigFile<{}> [fileName={}]", defaultObject.getClass().getSimpleName(), fileName);
        this.defaultObject = defaultObject;
        this.dataObject = defaultObject;
        this.file = new File(fileName);
        if(!file.exists()) {
            save();
        } else {
            load();
        }
    }

    public void load() {
        logger.debug("Loading Config File");
        try(FileReader fr = new FileReader(file);
            BufferedReader br = new BufferedReader(fr)) {
            String line;
            StringBuilder data = new StringBuilder();
            while ((line = br.readLine()) != null) {
                data.append(line);
            }
            this.dataObject = (T) gson.fromJson(data.toString(), defaultObject.getClass());
            if(this.dataObject == null) {
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
        try(FileWriter fw = new FileWriter(file)) {
            fw.write(gson.toJson(dataObject));
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
