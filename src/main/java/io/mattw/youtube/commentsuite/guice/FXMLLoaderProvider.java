package io.mattw.youtube.commentsuite.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import javafx.fxml.FXMLLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FXMLLoaderProvider implements Provider<FXMLLoader> {

    private static final Logger logger = LogManager.getLogger();

    @Inject
    Injector injector;

    @Override
    public FXMLLoader get() {
        logger.info("FXMLLoaderProvider.get()");
        FXMLLoader loader = new FXMLLoader();
        loader.setControllerFactory(p -> injector.getInstance(p));
        return loader;
    }

}
