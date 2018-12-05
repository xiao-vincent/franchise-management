package com.vince.retailmanager.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Entity
@Table(name = "payments")
@Data
@EqualsAndHashCode(callSuper = true, of = {""})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment extends BaseEntity {
	@Column
	@DecimalMin("0.00")
	@JsonView(View.Public.class)
	private BigDecimal amount;

	@Builder.Default
	@Column
	@JsonView(View.Public.class)
	private String currency = "USD";

	@OneToOne
	@JoinColumn(name = "sender_id")
	@NotNull
	@JsonView(View.Summary.class)
	private Company sender;

	@OneToOne
	@JoinColumn(name = "recipient_id")
	@NotNull
	@JsonIgnoreProperties("franchisees")
	@JsonView(View.Summary.class)
	private Company recipient;

//	@Setter(AccessLevel.NONE)
//	@JsonView(View.Public.class)
//	private boolean isRefund;

	@OneToOne
	@JsonView(View.Public.class)
	private Payment refundedPayment;

	@ManyToOne
	@JoinColumn(name = "invoice_id")
	@NotNull
	@JsonManagedReference
	@JsonView(View.Public.class)
	private Invoice invoice;


	public void addInvoice(Invoice invoice) {
		this.invoice = invoice;
		//check if this payment is a refund
//		if (getRecipient().equals(invoice.getCustomer()) && getSender().equals(invoice.getSeller())) {
//			isRefund = true;
//			invoice.getRefunds().add(this);
//		} else {
//			invoice.getPayments().add(this);
//		}
		if (refundedPayment == null) {
			invoice.getPayments().add(this);
		} else {
			invoice.getRefunds().add(this);
		}
	}


	public static class PaymentBuilder {
		public PaymentBuilder amount(Double amount) {
			this.amount = BigDecimal.valueOf(amount);
//			CurrencyUnit usd = CurrencyUnit.of("USD");
//			this.amount = Money.of(usd, amount);
			return this;
		}
	}

}
