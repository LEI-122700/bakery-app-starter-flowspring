package com.vaadin.starter.bakery.ui.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DebounceSettings;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.littemplate.LitTemplate;
import com.vaadin.flow.component.template.Id;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.dom.DebouncePhase;

/**
 * A reusable search bar web component implemented as a {@link LitTemplate}.
 * <p>
 * This component wraps a search input field, an optional checkbox,
 * and action/clear buttons. It emits custom events when the search
 * value or checkbox state changes, and allows external listeners to
 * react to user actions.
 *
 * <p>The HTML template is defined in
 * <code>src/components/search-bar.js</code>, and the element is
 * tagged with <code>&lt;search-bar&gt;</code>.
 */
@Tag("search-bar")
@JsModule("./src/components/search-bar.js")
public class SearchBar extends LitTemplate {

	@Id("field")
	private TextField textField;

	@Id("clear")
	private Button clearButton;

	@Id("action")
	private Button actionButton;


    /**
     * Creates a new {@code SearchBar} component.
     * <p>
     * Sets up:
     * <ul>
     *   <li>Eager value change mode on the search field.</li>
     *   <li>A listener that fires a {@link FilterChanged} event when the search value changes.</li>
     *   <li>A clear button that clears the text field and resets the checkbox property.</li>
     *   <li>A listener to detect checkbox state changes and fire {@link FilterChanged} events.</li>
     * </ul>
     */
	public SearchBar() {
		textField.setValueChangeMode(ValueChangeMode.EAGER);

		ComponentUtil.addListener(textField, SearchValueChanged.class,
				e -> fireEvent(new FilterChanged(this, false)));

		clearButton.addClickListener(e -> {
			textField.clear();
			getElement().setProperty("checkboxChecked", false);
		});

		getElement().addPropertyChangeListener("checkboxChecked", e -> fireEvent(new FilterChanged(this, false)));
	}

    /**
     * Gets the current search filter text.
     *
     * @return the text value entered in the search field
     */
	public String getFilter() {
		return textField.getValue();
	}

	@Synchronize("checkbox-checked-changed")
    /**
     * Checks whether the associated checkbox is selected.
     * <p>
     * The checkbox state is synchronized with the frontend
     * property {@code checkboxChecked}.
     *
     * @return {@code true} if the checkbox is checked, {@code false} otherwise
     */
	public boolean isCheckboxChecked() {
		return getElement().getProperty("checkboxChecked", false);
	}


    /**
     * Sets the placeholder text displayed inside the search field.
     *
     * @param placeHolder the placeholder text
     */
	public void setPlaceHolder(String placeHolder) {
		textField.setPlaceholder(placeHolder);
	}

    /**
     * Sets the text of the main action button.
     *
     * @param actionText the text to display on the action button
     */
	public void setActionText(String actionText) {
		getElement().setProperty("buttonText", actionText);
	}

    /**
     * Sets the text label for the checkbox.
     *
     * @param checkboxText the text to display next to the checkbox
     */
	public void setCheckboxText(String checkboxText) {
		getElement().setProperty("checkboxText", checkboxText);
	}

    /**
     * Adds a listener that is notified whenever the search value or
     * checkbox state changes.
     *
     * @param listener the listener to add
     */
	public void addFilterChangeListener(ComponentEventListener<FilterChanged> listener) {
		this.addListener(FilterChanged.class, listener);
	}

    /**
     * Adds a listener that is triggered when the action button is clicked.
     *
     * @param listener the listener to add
     */
	public void addActionClickListener(ComponentEventListener<ClickEvent<Button>> listener) {
		actionButton.addClickListener(listener);
	}

    /**
     * Gets the action button instance, allowing further customization.
     *
     * @return the {@link Button} used for actions
     */
	public Button getActionButton() {
		return actionButton;
	}

    /**
     * Event fired when the search value in the text field changes.
     * <p>
     * This event is debounced to avoid firing too frequently
     * during rapid typing.
     */
	@DomEvent(value = "value-changed", debounce = @DebounceSettings(timeout = 300, phases = DebouncePhase.TRAILING))
	public static class SearchValueChanged extends ComponentEvent<TextField> {
        /**
         * Creates a new {@code SearchValueChanged} event.
         *
         * @param source      the text field where the change occurred
         * @param fromClient  whether the event originated from the client side
         */
		public SearchValueChanged(TextField source, boolean fromClient) {
			super(source, fromClient);
		}
	}

    /**
     * Event fired whenever the filter changes — either the search text
     * value changes or the checkbox state changes.
     */
	public static class FilterChanged extends ComponentEvent<SearchBar> {
        /**
         * Creates a new {@code FilterChanged} event.
         *
         * @param source     the {@link SearchBar} that triggered the event
         * @param fromClient whether the event originated from the client side
         */
		public FilterChanged(SearchBar source, boolean fromClient) {
			super(source, fromClient);
		}
	}
}
