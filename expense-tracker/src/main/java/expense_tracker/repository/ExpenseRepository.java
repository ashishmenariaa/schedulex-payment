package expense_tracker.repository;

import java.time.LocalDate;
import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import expense_tracker.entity.Expense;


@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

	
	List<Expense> findByCategory(String category);
	List<Expense> findByDate(LocalDate date);
	List<Expense> findByDateBetween(LocalDate startDate, LocalDate endDate);

	List<Expense> findAllByOrderByDateDesc();
	
}
