package io.mattw.youtube.commentsuite;

import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/**
 * Modifies the height of empty "dummy" cells in a ListView.
 * Option to display tooltip on all cells to find preferred dummy cell height.
 * <p>
 * https://stackoverflow.com/a/46261347/2650847
 */
public class ListViewEmptyCellFactory<T extends Node> extends ListCell<T> {

    private double height = 25;
    private boolean tooltipHeight = false;
    private Tooltip tool = new Tooltip();

    public ListViewEmptyCellFactory() {
        tool.textProperty().bind(heightProperty().asString());
    }

    public ListViewEmptyCellFactory(final double height) {
        this(height, false);
    }

    public ListViewEmptyCellFactory(final double height, final boolean tooltipHeight) {
        this();
        this.height = height;
        this.tooltipHeight = tooltipHeight;
    }

    protected void updateItem(final T item, final boolean empty) {
        super.updateItem(item, empty);
        setTooltip(tooltipHeight ? tool : null);
        if (empty) {
            setPrefHeight(height);
            setGraphic(null);
        } else {
            setPrefHeight(Region.USE_COMPUTED_SIZE);
            setGraphic(item);
        }
    }
}
