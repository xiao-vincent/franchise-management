package com.vince.retailmanager.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.vince.retailmanager.exception.InvalidOperationException;
import com.vince.retailmanager.model.View.Summary;
import com.vince.retailmanager.model.entity.Company;
import com.vince.retailmanager.model.entity.IncomeStatement;
import com.vince.retailmanager.model.utils.IncomeStatementUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.ToString.Exclude;

@Getter
@ToString
@JsonView(Summary.class)
public class IncomeStatementStatistics {

  @JsonIgnore
  private final DateRange dateRange;

  @NotNull
  @JsonIgnore
  private final Company company;

  @JsonIgnore
  @Exclude
  private Collection<IncomeStatement> incomeStatements;

  private UnitStatistics sales;
  private UnitStatistics costOfGoodsSold;
  private UnitStatistics grossProfit;
  private UnitStatistics operatingExpenses;
  private UnitStatistics operatingIncome;
  private UnitStatistics generalAndAdminExpenses;
  private UnitStatistics netIncome;


  public static IncomeStatementStatistics create(Company company, DateRange dateRange) {
    IncomeStatementStatistics result = new IncomeStatementStatistics(company, dateRange);
    result.checkMissingIncomeStatements();
    result.init();
    return result;
  }

  private IncomeStatementStatistics(Company company, DateRange dateRange) {
    this.company = company;
    this.dateRange = dateRange;
  }

  private void init() {
    this.incomeStatements = IncomeStatementUtils
        .getSortedIncomeStatementsInDateRange(this.company, this.dateRange);
    this.sales = new UnitStatistics(IncomeStatement::getSales);
    this.costOfGoodsSold = new UnitStatistics(IncomeStatement::getCostOfGoodsSold);
    this.grossProfit = new UnitStatistics(IncomeStatement::getGrossProfit);
    this.operatingExpenses = new UnitStatistics(IncomeStatement::getOperatingExpenses);
    this.operatingIncome = new UnitStatistics(IncomeStatement::getOperatingIncome);
    this.generalAndAdminExpenses = new UnitStatistics(IncomeStatement::getGeneralAndAdminExpenses);
    this.netIncome = new UnitStatistics(IncomeStatement::getNetIncome);
  }

  private void checkMissingIncomeStatements() {
    Collection<LocalDate> missingDates = IncomeStatementUtils
        .getMissingIncomeStatementDates(this.company, this.dateRange);
    if (!missingDates.isEmpty()) {
      throw new InvalidOperationException(
          "Missing income statement dates in date range",
          Map.of("missingIncomeStatementDates", missingDates)
      );
    }
  }

  private Double sum(Function<IncomeStatement, BigDecimal> mapper) {
    return incomeStatements
        .stream()
        .map(mapper)
        .reduce(BigDecimal.ZERO, BigDecimal::add)
        .doubleValue()
        ;
  }

  @Getter
  @ToString
  @JsonView(Summary.class)
  class UnitStatistics {

    @Exclude
    @JsonIgnore
    private List<Double> values = new ArrayList<>();
    private double total;
    private double average;
    private double growthRate; //average annual growth rate
    private Entry min = new Entry(Double.MAX_VALUE);
    private Entry max = new Entry(Double.MIN_VALUE);

    UnitStatistics(Function<IncomeStatement, BigDecimal> mapper) {
      for (IncomeStatement incomeStatement : incomeStatements) {
        double value = mapper.apply(incomeStatement).doubleValue();
        values.add(value);

        this.total += value;

        //set min
        if (value < this.min.value) {
          this.min.setValue(value);
          this.min.setIncomeStatement(incomeStatement);
        }

        //set max
        if (value > this.max.value) {
          this.max.setValue(value);
          this.max.setIncomeStatement(incomeStatement);
        }
      }

      this.average = this.total / incomeStatements.size();
      this.growthRate = calculateGrowthRate(values);
    }

    private double calculateGrowthRate(List<Double> values) {
      List<Double> convertedDifferences = new ArrayList<>();
      for (int i = 0; i < values.size() - 1; i++) {
        if (values.get(i) <= 0) {
          return Double.MAX_VALUE;
        }
        double growthRate = (values.get(i + 1) - values.get(i)) / values.get(i);
        convertedDifferences.add(growthRate + 1);
      }
      assert convertedDifferences.size() == values.size() - 1;

      double product = convertedDifferences.stream()
          .reduce(1.0, (a, b) -> a * b);

      return Math.pow(product, 1.0 / convertedDifferences.size()) - 1;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonView(Summary.class)
    class Entry {

      @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
      @JsonIdentityReference(alwaysAsId = true)
      private IncomeStatement incomeStatement;
      private double value;

      Entry(double value) {
        this.value = value;
      }

      @Override
      public String toString() {
        return new StringJoiner(", ", Entry.class.getSimpleName() + "[", "]")
            .add("incomeStatement=" + incomeStatement.getDate())
            .add("value=" + value)
            .toString();
      }
    }

  }

}
