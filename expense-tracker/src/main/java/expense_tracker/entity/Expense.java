package expense_tracker.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "expense")
public class Expense {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Double amount;  // ✅ YEH ADD KARO!

	@Column(nullable = false)
	private String category;  // ✅ Spelling fix: cataegory → category

	@Column(nullable = false)
	private LocalDate date;

	private String description;

	// Getters and Setters

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Double getAmount() {  // ✅ YEH ADD KARO!
		return amount;
	}

	public void setAmount(Double amount) {  // ✅ YEH ADD KARO!
		this.amount = amount;
	}

	public String getCategory() {  // ✅ Spelling fix
		return category;
	}

	public void setCategory(String category) {  // ✅ Spelling fix
		this.category = category;
	}

	public LocalDate getDate() {
		return date;
	}

	public void setDate(LocalDate date) {
		this.date = date;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}