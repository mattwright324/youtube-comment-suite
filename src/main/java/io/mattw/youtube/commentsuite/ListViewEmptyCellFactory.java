package io.mattw.youtube.commentsuite;

import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Region;

/**
 * Modifies the height of empty "dummy" cells in a ListView.
 * Option to display tooltip on all cells to find preferred dummy cell height.
 *
 * https://stackoverflow.com/a/46261347/2650847
 *
 * @author mattwright324
 */
public class ListViewEmptyCellFactory<T extends Node> extends ListCell<T> {

    private double height = 25;
    private boolean tooltipHeight = false;
    private Tooltip tool = new Tooltip();

    public ListViewEmptyCellFactory() {
        tool.textProperty().bind(heightProperty().asString());
    }

    public ListViewEmptyCellFactory(double height) {
        this(height, false);
    }

    public ListViewEmptyCellFactory(double height, boolean tooltipHeight) {
        this();
        this.height = height;
        this.tooltipHeight = tooltipHeight;
    }

    protected void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);
        setTooltip(tooltipHeight ? tool : null);
        if(empty) {
            setPrefHeight(height);
            setGraphic(null);
        } else {
            setPrefHeight(Region.USE_COMPUTED_SIZE);
            setGraphic(item);
        }
    }
}
