package com.vaadin.starter.bakery.ui.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * A simple Vaadin {@link Grid} component displaying a list of integers
 * with a button in each row.
 * <p>
 * Each button, when clicked, opens a {@link RouteChangingDialog}.
 * <p>
 * This component is registered as a Spring bean with UI scope,
 * meaning a new instance is created for each user interface.
 */
@SpringComponent
@UIScope
public class GridComponent extends Grid<Integer> {

    /**
     * Creates a new {@code GridComponent} and configures it to display
     * two integer rows (0 and 1), each containing a button.
     */
    public GridComponent() {
        setItems(0, 1);
        addComponentColumn(i -> createButton(i));
    }

    /**
     * Creates a {@link Button} for a given integer value.
     * <p>
     * The button's caption will include the integer value
     * and clicking it opens a {@link RouteChangingDialog}.
     *
     * @param i the integer used in the button caption
     * @return a configured {@link Button} instance
     */
    private Button createButton(Integer i) {
        return new Button("Test Button " + i, e -> {
            RouteChangingDialog dialog = new RouteChangingDialog();
            dialog.open();
        });
    }

}
