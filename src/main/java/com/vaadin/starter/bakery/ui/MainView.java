package com.vaadin.starter.bakery.ui;

import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_DASHBOARD;
import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_LOGOUT;
import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_PRODUCTS;
import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_STOREFRONT;
import static com.vaadin.starter.bakery.ui.utils.BakeryConst.TITLE_USERS;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.starter.bakery.ui.utils.BakeryConst;
import com.vaadin.starter.bakery.ui.views.HasConfirmation;
import com.vaadin.starter.bakery.ui.views.admin.products.ProductsView;
import com.vaadin.starter.bakery.ui.views.admin.users.UsersView;
import com.vaadin.starter.bakery.ui.views.dashboard.DashboardView;
import com.vaadin.starter.bakery.ui.views.storefront.StorefrontView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

/**
 * The main view of the application, extending {@code AppLayout}.
 * <p>
 * This class handles the overall layout, navigation menu creation,
 * access control for views, and the logout process. It also manages a
 * global {@code ConfirmDialog} for views that implement {@code HasConfirmation}.
 */
public class MainView extends AppLayout {

	@Autowired
	private AccessAnnotationChecker accessChecker;
	private final ConfirmDialog confirmDialog = new ConfirmDialog();
	private Tabs menu;
	private static final String LOGOUT_SUCCESS_URL = "/" + BakeryConst.PAGE_ROOT;

	/**
	 * Initializes the view components, sets up the navigation menu,
	 * configures the global confirmation dialog, and registers event listeners
	 * for navbar visibility during search focus/blur.
	 * <p>
	 * This method is called automatically after dependency injection is complete.
	 */

	@PostConstruct
	public void init() {
		confirmDialog.setCancelable(true);
		confirmDialog.setConfirmButtonTheme("raised tertiary error");
		confirmDialog.setCancelButtonTheme("raised tertiary");

		this.setDrawerOpened(false);
		Span appName = new Span("###Bakery###");
		appName.addClassName("hide-on-mobile");

		menu = createMenuTabs();

		// Handle logout
		menu.addSelectedChangeListener(e -> {
			if (e.getSelectedTab() == null) {
				return;
			}
			
			e.getSelectedTab().getId().ifPresent(id -> {
				if ("logout-tab".equals(id)) {
					UI.getCurrent().getPage().setLocation(LOGOUT_SUCCESS_URL);
					SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
					logoutHandler.logout(
						VaadinServletRequest.getCurrent().getHttpServletRequest(), null,
					null);
				}
			});
		});

		this.addToNavbar(appName);
		this.addToNavbar(true, menu);
		this.getElement().appendChild(confirmDialog.getElement());

		getElement().addEventListener("search-focus", e -> {
			getElement().getClassList().add("hide-navbar");
		});

		getElement().addEventListener("search-blur", e -> {
			getElement().getClassList().remove("hide-navbar");
		});
	}

	/**
	 * Called after navigation to a new view.
	 * <p>
	 * This method closes the global {@code ConfirmDialog}, injects the dialog
	 * into the newly navigated view if it implements {@code HasConfirmation},
	 * and ensures the correct tab in the navigation menu is selected based
	 * on the current view's route.
	 */

	@Override
	protected void afterNavigation() {
		super.afterNavigation();
		confirmDialog.setOpened(false);
		if (getContent() instanceof HasConfirmation) {
			((HasConfirmation) getContent()).setConfirmDialog(confirmDialog);
		}
		RouteConfiguration configuration = RouteConfiguration.forSessionScope();
		if (configuration.isRouteRegistered(this.getContent().getClass())) {
			String target = configuration.getUrl(this.getContent().getClass());
			Optional<Component> tabToSelect = menu.getChildren().filter(tab -> {
				Component child = tab.getChildren().findFirst().get();
				return child instanceof RouterLink && ((RouterLink) child).getHref().equals(target);
			}).findFirst();
			tabToSelect.ifPresent(tab -> menu.setSelectedTab((Tab) tab));
		} else {
			menu.setSelectedTab(null);
		}
	}

	/**
	 * Creates the main navigation tabs component.
	 *
	 * @return a {@code Tabs} component containing the available menu tabs.
	 */

	private Tabs createMenuTabs() {
		final Tabs tabs = new Tabs();
		tabs.setOrientation(Tabs.Orientation.HORIZONTAL);
		tabs.add(getAvailableTabs());
		return tabs;
	}

	/**
	 * Determines and creates the array of available navigation tabs for the user.
	 * <p>
	 * Access to the {@code UsersView} and {@code ProductsView} tabs is checked
	 * using the {@code AccessAnnotationChecker}. A logout tab is always included.
	 *
	 * @return an array of {@code Tab} objects accessible to the current user.
	 */

	private Tab[] getAvailableTabs() {
		final List<Tab> tabs = new ArrayList<>(4);
		tabs.add(createTab(VaadinIcon.EDIT, TITLE_STOREFRONT, StorefrontView.class));
		tabs.add(createTab(VaadinIcon.CLOCK, TITLE_DASHBOARD, DashboardView.class));
		if (accessChecker.hasAccess(UsersView.class,
				VaadinServletRequest.getCurrent().getHttpServletRequest())) {
			tabs.add(createTab(VaadinIcon.USER, TITLE_USERS, UsersView.class));
		}
		if (accessChecker.hasAccess(ProductsView.class,
				VaadinServletRequest.getCurrent().getHttpServletRequest())) {
			tabs.add(createTab(VaadinIcon.CALENDAR, TITLE_PRODUCTS, ProductsView.class));
		}
		final String contextPath = VaadinServlet.getCurrent().getServletContext().getContextPath();
		final Tab logoutTab = createTab(createLogoutLink(contextPath));
		logoutTab.setId("logout-tab");
		tabs.add(logoutTab);
		return tabs.toArray(new Tab[tabs.size()]);
	}

	/**
	 * Creates a navigation tab with an icon and title that links to a specific view class.
	 *
	 * @param icon      The {@code VaadinIcon} to display in the tab.
	 * @param title     The text title to display in the tab.
	 * @param viewClass The {@code Class} of the view this tab navigates to.
	 * @return A newly created {@code Tab} object.
	 */

	private static Tab createTab(VaadinIcon icon, String title, Class<? extends Component> viewClass) {
		return createTab(populateLink(new RouterLink("", viewClass), icon, title));
	}

	/**
	 * Wraps a given component in a standard styled {@code Tab}.
	 *
	 * @param content The component to be placed inside the tab (e.g., a {@code RouterLink} or {@code Anchor}).
	 * @return A newly created {@code Tab} object with {@code LUMO_ICON_ON_TOP} theme variant.
	 */

	private static Tab createTab(Component content) {
		final Tab tab = new Tab();
		tab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
		tab.add(content);
		return tab;
	}

	/**
	 * Creates an {@code Anchor} component styled as a logout link.
	 *
	 * @param contextPath The context path of the application.
	 * @return A styled {@code Anchor} for logging out.
	 */

	private static Anchor createLogoutLink(String contextPath) {
		final Anchor a = populateLink(new Anchor(), VaadinIcon.ARROW_RIGHT, TITLE_LOGOUT);
		return a;
	}

	/**
	 * Populates a component that implements {@code HasComponents} (like {@code Anchor} or {@code RouterLink})
	 * with an icon and a title.
	 *
	 * @param <T>   The type of the component, which must implement {@code HasComponents}.
	 * @param a     The component to populate.
	 * @param icon  The {@code VaadinIcon} to add.
	 * @param title The text title to add.
	 * @return The populated component {@code a}.
	 */

	private static <T extends HasComponents> T populateLink(T a, VaadinIcon icon, String title) {
		a.add(icon.create());
		a.add(title);
		return a;
	}
}