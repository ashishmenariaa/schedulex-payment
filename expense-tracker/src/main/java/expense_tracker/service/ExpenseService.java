package expense_tracker.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import expense_tracker.entity.Expense;
import expense_tracker.repository.ExpenseRepository;

@Service
public class ExpenseService {
	
	@Autowired
	private ExpenseRepository expenseRepository;
	
	public Expense addExpense(Expense expense) {
		return expenseRepository.save(expense);
	}
	
	public List<Expense> getAllExpenses() {
		return expenseRepository.findAll();
	}
	
	public Expense getExpenseById(Long id) {
		Optional<Expense> expense = expenseRepository.findById(id);
		return expense.orElse(null);
	}
	
	public Expense updateExpense(Long id, Expense updatedExpense) {
		if(expenseRepository.existsById(id)) {
			updatedExpense.setId(id);
			return expenseRepository.save(updatedExpense);
		}
		return null;
	}
	
	public boolean deleteExpense(Long id) {
		if(expenseRepository.existsById(id)) {
			expenseRepository.deleteById(id);
			return true;
		}
		return false;
	}
	
	public List<Expense> getExpensesByCategory(String category) {
		return expenseRepository.findByCategory(category);
	}
	
	public List<Expense> getExpensesByDate(LocalDate date) {
		return expenseRepository.findByDate(date);
	}
	
	public Double getTotalAmount() {
		List<Expense> expenses = expenseRepository.findAll();
		return expenses.stream()
				.mapToDouble(Expense::getAmount)
				.sum();
	}
	
	public Double getTotalByCategory(String category) {
		List<Expense> expenses = expenseRepository.findByCategory(category);
		return expenses.stream()
				.mapToDouble(Expense::getAmount)
				.sum();
	}
}