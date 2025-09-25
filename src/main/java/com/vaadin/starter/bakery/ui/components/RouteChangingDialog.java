package com.vaadin.starter.bakery.ui.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;

/**
 * A custom {@link Dialog} that can modify the browser history when opened
 * and allows the user to navigate back.
 * <p>
 * When the dialog is opened, it pushes a new history state with the
 * path {@code "home"} to the browser. It also displays a "Back" button
 * that, when clicked, navigates the user back to the previous page and
 * closes the dialog.
 */
public class RouteChangingDialog extends Dialog {


    /**
     * Creates a new {@code RouteChangingDialog}.
     * <p>
     * The dialog contains:
     * <ul>
     *   <li>A "Back" button that calls {@link UI#getCurrent()} to navigate
     *   the browser history one step back and then closes the dialog.</li>
     *   <li>An opened change listener that pushes a new browser history
     *   state pointing to {@code "home"} whenever the dialog is opened.</li>
     * </ul>
     */
    public RouteChangingDialog() {
        Button backButton = new Button("Back", e -> {
            UI.getCurrent().getPage().getHistory().back();
            close();
        });
        add(backButton);

        addOpenedChangeListener(e -> {
            if (isOpened())
                UI.getCurrent().getPage().getHistory().pushState(null, "home");
        });
    }
}