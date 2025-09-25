package com.vaadin.starter.bakery.backend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.vaadin.starter.bakery.backend.data.DashboardData;
import com.vaadin.starter.bakery.backend.data.DeliveryStats;
import com.vaadin.starter.bakery.backend.data.OrderState;
import com.vaadin.starter.bakery.backend.data.entity.Order;
import com.vaadin.starter.bakery.backend.data.entity.OrderSummary;
import com.vaadin.starter.bakery.backend.data.entity.Product;
import com.vaadin.starter.bakery.backend.data.entity.User;
import com.vaadin.starter.bakery.backend.repositories.OrderRepository;

/**
 * Service class for managing {@link Order} entities.
 * Provides CRUD operations, queries, statistics, and dashboard data aggregation.
 */
@Service
public class OrderService implements CrudService<Order> {

	private final OrderRepository orderRepository;

	/**
	 * Creates a new {@code OrderService}.
	 *
	 * @param orderRepository the repository used to access orders
	 */
	@Autowired
	public OrderService(OrderRepository orderRepository) {
		super();
		this.orderRepository = orderRepository;
	}

	/**
	 * States in which orders are not available for delivery.
	 */
	private static final Set<OrderState> notAvailableStates = Collections.unmodifiableSet(
			EnumSet.complementOf(EnumSet.of(OrderState.DELIVERED, OrderState.READY, OrderState.CANCELLED)));

	/**
	 * Creates or updates an order, filling its details using the provided consumer.
	 *
	 * @param currentUser the user performing the operation
	 * @param id          the id of the order to update, or {@code null} to create a new order
	 * @param orderFiller consumer used to fill or update order details
	 * @return the saved order
	 */
	@Transactional(rollbackOn = Exception.class)
	public Order saveOrder(User currentUser, Long id, BiConsumer<User, Order> orderFiller) {
		Order order;
		if (id == null) {
			order = new Order(currentUser);
		} else {
			order = load(id);
		}
		orderFiller.accept(currentUser, order);
		return orderRepository.save(order);
	}

	/**
	 * Saves the given order.
	 *
	 * @param order the order to save
	 * @return the saved order
	 */
	@Transactional(rollbackOn = Exception.class)
	public Order saveOrder(Order order) {
		return orderRepository.save(order);
	}

	/**
	 * Adds a comment to an order’s history and saves it.
	 *
	 * @param currentUser the user adding the comment
	 * @param order       the order being updated
	 * @param comment     the comment text
	 * @return the updated order
	 */
	@Transactional(rollbackOn = Exception.class)
	public Order addComment(User currentUser, Order order, String comment) {
		order.addHistoryItem(currentUser, comment);
		return orderRepository.save(order);
	}

	/**
	 * Finds orders that match a name filter and have a due date after a given date.
	 *
	 * @param optionalFilter     optional customer name filter
	 * @param optionalFilterDate optional date filter
	 * @param pageable           pagination information
	 * @return a page of matching orders
	 */
	public Page<Order> findAnyMatchingAfterDueDate(Optional<String> optionalFilter,
												   Optional<LocalDate> optionalFilterDate, Pageable pageable) {
		if (optionalFilter.isPresent() && !optionalFilter.get().isEmpty()) {
			if (optionalFilterDate.isPresent()) {
				return orderRepository.findByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(
						optionalFilter.get(), optionalFilterDate.get(), pageable);
			} else {
				return orderRepository.findByCustomerFullNameContainingIgnoreCase(optionalFilter.get(), pageable);
			}
		} else {
			if (optionalFilterDate.isPresent()) {
				return orderRepository.findByDueDateAfter(optionalFilterDate.get(), pageable);
			} else {
				return orderRepository.findAll(pageable);
			}
		}
	}

	/**
	 * Finds all orders starting from today onwards.
	 *
	 * @return a list of order summaries starting today
	 */
	@Transactional
	public List<OrderSummary> findAnyMatchingStartingToday() {
		return orderRepository.findByDueDateGreaterThanEqual(LocalDate.now());
	}

	/**
	 * Counts orders that match the given name filter and due date filter.
	 *
	 * @param optionalFilter     optional customer name filter
	 * @param optionalFilterDate optional due date filter
	 * @return the number of matching orders
	 */
	public long countAnyMatchingAfterDueDate(Optional<String> optionalFilter, Optional<LocalDate> optionalFilterDate) {
		if (optionalFilter.isPresent() && optionalFilterDate.isPresent()) {
			return orderRepository.countByCustomerFullNameContainingIgnoreCaseAndDueDateAfter(optionalFilter.get(),
					optionalFilterDate.get());
		} else if (optionalFilter.isPresent()) {
			return orderRepository.countByCustomerFullNameContainingIgnoreCase(optionalFilter.get());
		} else if (optionalFilterDate.isPresent()) {
			return orderRepository.countByDueDateAfter(optionalFilterDate.get());
		} else {
			return orderRepository.count();
		}
	}

	/**
	 * Builds and returns delivery statistics for the dashboard.
	 *
	 * @return the delivery statistics
	 */
	private DeliveryStats getDeliveryStats() {
		DeliveryStats stats = new DeliveryStats();
		LocalDate today = LocalDate.now();
		stats.setDueToday((int) orderRepository.countByDueDate(today));
		stats.setDueTomorrow((int) orderRepository.countByDueDate(today.plusDays(1)));
		stats.setDeliveredToday((int) orderRepository.countByDueDateAndStateIn(today,
				Collections.singleton(OrderState.DELIVERED)));

		stats.setNotAvailableToday((int) orderRepository.countByDueDateAndStateIn(today, notAvailableStates));
		stats.setNewOrders((int) orderRepository.countByState(OrderState.NEW));

		return stats;
	}

	/**
	 * Retrieves dashboard data including delivery statistics, sales, and product deliveries.
	 *
	 * @param month the month to retrieve data for
	 * @param year  the year to retrieve data for
	 * @return the dashboard data
	 */
	public DashboardData getDashboardData(int month, int year) {
		DashboardData data = new DashboardData();
		data.setDeliveryStats(getDeliveryStats());
		data.setDeliveriesThisMonth(getDeliveriesPerDay(month, year));
		data.setDeliveriesThisYear(getDeliveriesPerMonth(year));

		Number[][] salesPerMonth = new Number[3][12];
		data.setSalesPerMonth(salesPerMonth);
		List<Object[]> sales = orderRepository.sumPerMonthLastThreeYears(OrderState.DELIVERED, year);

		for (Object[] salesData : sales) {
			// year, month, deliveries
			int y = year - (int) salesData[0];
			int m = (int) salesData[1] - 1;
			if (y == 0 && m == month - 1) {
				// skip current month as it contains incomplete data
				continue;
			}
			long count = (long) salesData[2];
			salesPerMonth[y][m] = count;
		}

		LinkedHashMap<Product, Integer> productDeliveries = new LinkedHashMap<>();
		data.setProductDeliveries(productDeliveries);
		for (Object[] result : orderRepository.countPerProduct(OrderState.DELIVERED, year, month)) {
			int sum = ((Long) result[0]).intValue();
			Product p = (Product) result[1];
			productDeliveries.put(p, sum);
		}

		return data;
	}

	/**
	 * Retrieves the number of deliveries per day for a given month and year.
	 *
	 * @param month the month
	 * @param year  the year
	 * @return a list of deliveries per day, with {@code null} for days without deliveries
	 */
	private List<Number> getDeliveriesPerDay(int month, int year) {
		int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
		return flattenAndReplaceMissingWithNull(daysInMonth,
				orderRepository.countPerDay(OrderState.DELIVERED, year, month));
	}

	/**
	 * Retrieves the number of deliveries per month for a given year.
	 *
	 * @param year the year
	 * @return a list of deliveries per month, with {@code null} for months without deliveries
	 */
	private List<Number> getDeliveriesPerMonth(int year) {
		return flattenAndReplaceMissingWithNull(12, orderRepository.countPerMonth(OrderState.DELIVERED, year));
	}

	/**
	 * Normalizes query results into a fixed-length list of numbers, replacing missing values with {@code null}.
	 *
	 * @param length the expected length of the result list
	 * @param list   the raw query result
	 * @return a list of numbers with missing values replaced by {@code null}
	 */
	private List<Number> flattenAndReplaceMissingWithNull(int length, List<Object[]> list) {
		List<Number> counts = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			counts.add(null);
		}

		for (Object[] result : list) {
			counts.set((Integer) result[0] - 1, (Number) result[1]);
		}
		return counts;
	}

	/**
	 * Returns the repository used for accessing orders.
	 *
	 * @return the order repository
	 */
	@Override
	public JpaRepository<Order, Long> getRepository() {
		return orderRepository;
	}

	/**
	 * Creates a new {@link Order} with default due date and time.
	 *
	 * @param currentUser the user creating the order
	 * @return a new order instance
	 */
	@Override
	@Transactional
	public Order createNew(User currentUser) {
		Order order = new Order(currentUser);
		order.setDueTime(LocalTime.of(16, 0));
		order.setDueDate(LocalDate.now());
		return order;
	}

}
