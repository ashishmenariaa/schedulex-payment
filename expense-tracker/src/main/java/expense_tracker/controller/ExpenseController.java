package expense_tracker.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import expense_tracker.entity.Expense;
import expense_tracker.service.ExpenseService;


@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

	@Autowired
	private ExpenseService expenseService;

	@GetMapping
	public List<Expense> getAllExpenses() {
		return expenseService.getAllExpenses();
	}

	@GetMapping("/{id}")
	public Expense getExpenseById(@PathVariable Long id) {
		return expenseService.getExpenseById(id);
	}

	@PostMapping
	public Expense addExpense(@RequestBody Expense expense) {
		return expenseService.addExpense(expense);
	}

	@PutMapping("/{id}")
	public Expense updateExpense(@PathVariable Long id, @RequestBody Expense expense) {
		return expenseService.updateExpense(id, expense);
	}

	@DeleteMapping("/{id}")
	public String deleteExpense(@PathVariable Long id) {
		boolean deleted = expenseService.deleteExpense(id);
		if(deleted) {
			return "Expense deleted successfully!";
		}
		return "Expense not found!";
	}

	@GetMapping("/category/{category}")
	public List<Expense> getExpensesByCategory(@PathVariable String category) {
		return expenseService.getExpensesByCategory(category);
	}

	@GetMapping("/total")
	public Double getTotalAmount() {
		return expenseService.getTotalAmount();
	}

	@GetMapping("/total/{category}")
	public Double getTotalByCategory(@PathVariable String category) {
		return expenseService.getTotalByCategory(category);
	}
}