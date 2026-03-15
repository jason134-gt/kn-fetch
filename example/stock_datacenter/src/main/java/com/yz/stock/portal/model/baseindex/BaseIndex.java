package com.yz.stock.portal.model.baseindex;

import java.io.Serializable;
import java.util.Date;

public class BaseIndex implements Serializable {
    private Long id;

    private String companyCode;

    private Date lastData;

    private Double profitIncreaseIncomeIncrease=0.0;

    private Double expenseIncreaseIncomeIncrease=0.0;

    private Double sellGrossProfit=0.0;

    private Double sellFinalProfit=0.0;

    private Double netAssetYieldRate=0.0;

    private Double totalAssetYieldRate=0.0;

    private Double theoryBillTurnoverRate=0.0;

    private Double stockTurnoverRate=0.0;

    private Double flowRate=0.0;

    private Double quickrate=0.0;

    private Double assetIndebtedRate=0.0;

    private Double finalIndebetedRate=0.0;

    private Double otherRate=0.0;

    private Double profitPermanmentAssetRatio=0.0;

    private Double adverExpenseIncomeRatio=0.0;

    private Double customerEvalueRatio=0.0;

    private Double deriveToolRiskRatio=0.0;

    private Double latestPrice=0.0;

    private Double totalPrice=0.0;

    private Double earningPerPart=0.0;

    private Double assetPerPart=0.0;

    private Double cashFlowPerPart=0.0;

    private Double totalPriceFreeCashFlow=0.0;

    private Double evFreeCaseFlow=0.0;

    private Double evEbitda=0.0;

    private Double priceRatio=0.0;

    private Double finalRatio=0.0;

    private Double ttm=0.0;

    private Double staticPeg=0.0;

    private Double dynamicPeg=0.0;

    private Double avrIncreaseRatioYear=0.0;

    private Date time;

    private Date updateTime;

    private Double expectFutureIncreaseRatio=0.0;

    private static final long serialVersionUID = 1L;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode == null ? null : companyCode.trim();
    }

    public Date getLastData() {
        return lastData;
    }

    public void setLastData(Date lastData) {
        this.lastData = lastData;
    }

    public Double getProfitIncreaseIncomeIncrease() {
        return profitIncreaseIncomeIncrease;
    }

    public void setProfitIncreaseIncomeIncrease(Double profitIncreaseIncomeIncrease) {
        this.profitIncreaseIncomeIncrease = profitIncreaseIncomeIncrease;
    }

    public Double getExpenseIncreaseIncomeIncrease() {
        return expenseIncreaseIncomeIncrease;
    }

    public void setExpenseIncreaseIncomeIncrease(Double expenseIncreaseIncomeIncrease) {
        this.expenseIncreaseIncomeIncrease = expenseIncreaseIncomeIncrease;
    }

    public Double getSellGrossProfit() {
        return sellGrossProfit;
    }

    public void setSellGrossProfit(Double sellGrossProfit) {
        this.sellGrossProfit = sellGrossProfit;
    }

    public Double getSellFinalProfit() {
        return sellFinalProfit;
    }

    public void setSellFinalProfit(Double sellFinalProfit) {
        this.sellFinalProfit = sellFinalProfit;
    }

    public Double getNetAssetYieldRate() {
        return netAssetYieldRate;
    }

    public void setNetAssetYieldRate(Double netAssetYieldRate) {
        this.netAssetYieldRate = netAssetYieldRate;
    }

    public Double getTotalAssetYieldRate() {
        return totalAssetYieldRate;
    }

    public void setTotalAssetYieldRate(Double totalAssetYieldRate) {
        this.totalAssetYieldRate = totalAssetYieldRate;
    }

    public Double getTheoryBillTurnoverRate() {
        return theoryBillTurnoverRate;
    }

    public void setTheoryBillTurnoverRate(Double theoryBillTurnoverRate) {
        this.theoryBillTurnoverRate = theoryBillTurnoverRate;
    }

    public Double getStockTurnoverRate() {
        return stockTurnoverRate;
    }

    public void setStockTurnoverRate(Double stockTurnoverRate) {
        this.stockTurnoverRate = stockTurnoverRate;
    }

    public Double getFlowRate() {
        return flowRate;
    }

    public void setFlowRate(Double flowRate) {
        this.flowRate = flowRate;
    }

    public Double getQuickrate() {
        return quickrate;
    }

    public void setQuickrate(Double quickrate) {
        this.quickrate = quickrate;
    }

    public Double getAssetIndebtedRate() {
        return assetIndebtedRate;
    }

    public void setAssetIndebtedRate(Double assetIndebtedRate) {
        this.assetIndebtedRate = assetIndebtedRate;
    }

    public Double getFinalIndebetedRate() {
        return finalIndebetedRate;
    }

    public void setFinalIndebetedRate(Double finalIndebetedRate) {
        this.finalIndebetedRate = finalIndebetedRate;
    }

    public Double getOtherRate() {
        return otherRate;
    }

    public void setOtherRate(Double otherRate) {
        this.otherRate = otherRate;
    }

    public Double getProfitPermanmentAssetRatio() {
        return profitPermanmentAssetRatio;
    }

    public void setProfitPermanmentAssetRatio(Double profitPermanmentAssetRatio) {
        this.profitPermanmentAssetRatio = profitPermanmentAssetRatio;
    }

    public Double getAdverExpenseIncomeRatio() {
        return adverExpenseIncomeRatio;
    }

    public void setAdverExpenseIncomeRatio(Double adverExpenseIncomeRatio) {
        this.adverExpenseIncomeRatio = adverExpenseIncomeRatio;
    }

    public Double getCustomerEvalueRatio() {
        return customerEvalueRatio;
    }

    public void setCustomerEvalueRatio(Double customerEvalueRatio) {
        this.customerEvalueRatio = customerEvalueRatio;
    }

    public Double getDeriveToolRiskRatio() {
        return deriveToolRiskRatio;
    }

    public void setDeriveToolRiskRatio(Double deriveToolRiskRatio) {
        this.deriveToolRiskRatio = deriveToolRiskRatio;
    }

    public Double getLatestPrice() {
        return latestPrice;
    }

    public void setLatestPrice(Double latestPrice) {
        this.latestPrice = latestPrice;
    }

    public Double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(Double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public Double getEarningPerPart() {
        return earningPerPart;
    }

    public void setEarningPerPart(Double earningPerPart) {
        this.earningPerPart = earningPerPart;
    }

    public Double getAssetPerPart() {
        return assetPerPart;
    }

    public void setAssetPerPart(Double assetPerPart) {
        this.assetPerPart = assetPerPart;
    }

    public Double getCashFlowPerPart() {
        return cashFlowPerPart;
    }

    public void setCashFlowPerPart(Double cashFlowPerPart) {
        this.cashFlowPerPart = cashFlowPerPart;
    }

    public Double getTotalPriceFreeCashFlow() {
        return totalPriceFreeCashFlow;
    }

    public void setTotalPriceFreeCashFlow(Double totalPriceFreeCashFlow) {
        this.totalPriceFreeCashFlow = totalPriceFreeCashFlow;
    }

    public Double getEvFreeCaseFlow() {
        return evFreeCaseFlow;
    }

    public void setEvFreeCaseFlow(Double evFreeCaseFlow) {
        this.evFreeCaseFlow = evFreeCaseFlow;
    }

    public Double getEvEbitda() {
        return evEbitda;
    }

    public void setEvEbitda(Double evEbitda) {
        this.evEbitda = evEbitda;
    }

    public Double getPriceRatio() {
        return priceRatio;
    }

    public void setPriceRatio(Double priceRatio) {
        this.priceRatio = priceRatio;
    }

    public Double getFinalRatio() {
        return finalRatio;
    }

    public void setFinalRatio(Double finalRatio) {
        this.finalRatio = finalRatio;
    }

    public Double getTtm() {
        return ttm;
    }

    public void setTtm(Double ttm) {
        this.ttm = ttm;
    }

    public Double getStaticPeg() {
        return staticPeg;
    }

    public void setStaticPeg(Double staticPeg) {
        this.staticPeg = staticPeg;
    }

    public Double getDynamicPeg() {
        return dynamicPeg;
    }

    public void setDynamicPeg(Double dynamicPeg) {
        this.dynamicPeg = dynamicPeg;
    }

    public Double getAvrIncreaseRatioYear() {
        return avrIncreaseRatioYear;
    }

    public void setAvrIncreaseRatioYear(Double avrIncreaseRatioYear) {
        this.avrIncreaseRatioYear = avrIncreaseRatioYear;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Double getExpectFutureIncreaseRatio() {
        return expectFutureIncreaseRatio;
    }

    public void setExpectFutureIncreaseRatio(Double expectFutureIncreaseRatio) {
        this.expectFutureIncreaseRatio = expectFutureIncreaseRatio;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null) {
            return false;
        }
        if (getClass() != that.getClass()) {
            return false;
        }
        BaseIndex other = (BaseIndex) that;
        return (this.getId() == null ? other.getId() == null : this.getId().equals(other.getId()))
            && (this.getCompanyCode() == null ? other.getCompanyCode() == null : this.getCompanyCode().equals(other.getCompanyCode()))
            && (this.getLastData() == null ? other.getLastData() == null : this.getLastData().equals(other.getLastData()))
            && (this.getProfitIncreaseIncomeIncrease() == null ? other.getProfitIncreaseIncomeIncrease() == null : this.getProfitIncreaseIncomeIncrease().equals(other.getProfitIncreaseIncomeIncrease()))
            && (this.getExpenseIncreaseIncomeIncrease() == null ? other.getExpenseIncreaseIncomeIncrease() == null : this.getExpenseIncreaseIncomeIncrease().equals(other.getExpenseIncreaseIncomeIncrease()))
            && (this.getSellGrossProfit() == null ? other.getSellGrossProfit() == null : this.getSellGrossProfit().equals(other.getSellGrossProfit()))
            && (this.getSellFinalProfit() == null ? other.getSellFinalProfit() == null : this.getSellFinalProfit().equals(other.getSellFinalProfit()))
            && (this.getNetAssetYieldRate() == null ? other.getNetAssetYieldRate() == null : this.getNetAssetYieldRate().equals(other.getNetAssetYieldRate()))
            && (this.getTotalAssetYieldRate() == null ? other.getTotalAssetYieldRate() == null : this.getTotalAssetYieldRate().equals(other.getTotalAssetYieldRate()))
            && (this.getTheoryBillTurnoverRate() == null ? other.getTheoryBillTurnoverRate() == null : this.getTheoryBillTurnoverRate().equals(other.getTheoryBillTurnoverRate()))
            && (this.getStockTurnoverRate() == null ? other.getStockTurnoverRate() == null : this.getStockTurnoverRate().equals(other.getStockTurnoverRate()))
            && (this.getFlowRate() == null ? other.getFlowRate() == null : this.getFlowRate().equals(other.getFlowRate()))
            && (this.getQuickrate() == null ? other.getQuickrate() == null : this.getQuickrate().equals(other.getQuickrate()))
            && (this.getAssetIndebtedRate() == null ? other.getAssetIndebtedRate() == null : this.getAssetIndebtedRate().equals(other.getAssetIndebtedRate()))
            && (this.getFinalIndebetedRate() == null ? other.getFinalIndebetedRate() == null : this.getFinalIndebetedRate().equals(other.getFinalIndebetedRate()))
            && (this.getOtherRate() == null ? other.getOtherRate() == null : this.getOtherRate().equals(other.getOtherRate()))
            && (this.getProfitPermanmentAssetRatio() == null ? other.getProfitPermanmentAssetRatio() == null : this.getProfitPermanmentAssetRatio().equals(other.getProfitPermanmentAssetRatio()))
            && (this.getAdverExpenseIncomeRatio() == null ? other.getAdverExpenseIncomeRatio() == null : this.getAdverExpenseIncomeRatio().equals(other.getAdverExpenseIncomeRatio()))
            && (this.getCustomerEvalueRatio() == null ? other.getCustomerEvalueRatio() == null : this.getCustomerEvalueRatio().equals(other.getCustomerEvalueRatio()))
            && (this.getDeriveToolRiskRatio() == null ? other.getDeriveToolRiskRatio() == null : this.getDeriveToolRiskRatio().equals(other.getDeriveToolRiskRatio()))
            && (this.getLatestPrice() == null ? other.getLatestPrice() == null : this.getLatestPrice().equals(other.getLatestPrice()))
            && (this.getTotalPrice() == null ? other.getTotalPrice() == null : this.getTotalPrice().equals(other.getTotalPrice()))
            && (this.getEarningPerPart() == null ? other.getEarningPerPart() == null : this.getEarningPerPart().equals(other.getEarningPerPart()))
            && (this.getAssetPerPart() == null ? other.getAssetPerPart() == null : this.getAssetPerPart().equals(other.getAssetPerPart()))
            && (this.getCashFlowPerPart() == null ? other.getCashFlowPerPart() == null : this.getCashFlowPerPart().equals(other.getCashFlowPerPart()))
            && (this.getTotalPriceFreeCashFlow() == null ? other.getTotalPriceFreeCashFlow() == null : this.getTotalPriceFreeCashFlow().equals(other.getTotalPriceFreeCashFlow()))
            && (this.getEvFreeCaseFlow() == null ? other.getEvFreeCaseFlow() == null : this.getEvFreeCaseFlow().equals(other.getEvFreeCaseFlow()))
            && (this.getEvEbitda() == null ? other.getEvEbitda() == null : this.getEvEbitda().equals(other.getEvEbitda()))
            && (this.getPriceRatio() == null ? other.getPriceRatio() == null : this.getPriceRatio().equals(other.getPriceRatio()))
            && (this.getFinalRatio() == null ? other.getFinalRatio() == null : this.getFinalRatio().equals(other.getFinalRatio()))
            && (this.getTtm() == null ? other.getTtm() == null : this.getTtm().equals(other.getTtm()))
            && (this.getStaticPeg() == null ? other.getStaticPeg() == null : this.getStaticPeg().equals(other.getStaticPeg()))
            && (this.getDynamicPeg() == null ? other.getDynamicPeg() == null : this.getDynamicPeg().equals(other.getDynamicPeg()))
            && (this.getAvrIncreaseRatioYear() == null ? other.getAvrIncreaseRatioYear() == null : this.getAvrIncreaseRatioYear().equals(other.getAvrIncreaseRatioYear()))
            && (this.getTime() == null ? other.getTime() == null : this.getTime().equals(other.getTime()))
            && (this.getUpdateTime() == null ? other.getUpdateTime() == null : this.getUpdateTime().equals(other.getUpdateTime()))
            && (this.getExpectFutureIncreaseRatio() == null ? other.getExpectFutureIncreaseRatio() == null : this.getExpectFutureIncreaseRatio().equals(other.getExpectFutureIncreaseRatio()));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getId() == null) ? 0 : getId().hashCode());
        result = prime * result + ((getCompanyCode() == null) ? 0 : getCompanyCode().hashCode());
        result = prime * result + ((getLastData() == null) ? 0 : getLastData().hashCode());
        result = prime * result + ((getProfitIncreaseIncomeIncrease() == null) ? 0 : getProfitIncreaseIncomeIncrease().hashCode());
        result = prime * result + ((getExpenseIncreaseIncomeIncrease() == null) ? 0 : getExpenseIncreaseIncomeIncrease().hashCode());
        result = prime * result + ((getSellGrossProfit() == null) ? 0 : getSellGrossProfit().hashCode());
        result = prime * result + ((getSellFinalProfit() == null) ? 0 : getSellFinalProfit().hashCode());
        result = prime * result + ((getNetAssetYieldRate() == null) ? 0 : getNetAssetYieldRate().hashCode());
        result = prime * result + ((getTotalAssetYieldRate() == null) ? 0 : getTotalAssetYieldRate().hashCode());
        result = prime * result + ((getTheoryBillTurnoverRate() == null) ? 0 : getTheoryBillTurnoverRate().hashCode());
        result = prime * result + ((getStockTurnoverRate() == null) ? 0 : getStockTurnoverRate().hashCode());
        result = prime * result + ((getFlowRate() == null) ? 0 : getFlowRate().hashCode());
        result = prime * result + ((getQuickrate() == null) ? 0 : getQuickrate().hashCode());
        result = prime * result + ((getAssetIndebtedRate() == null) ? 0 : getAssetIndebtedRate().hashCode());
        result = prime * result + ((getFinalIndebetedRate() == null) ? 0 : getFinalIndebetedRate().hashCode());
        result = prime * result + ((getOtherRate() == null) ? 0 : getOtherRate().hashCode());
        result = prime * result + ((getProfitPermanmentAssetRatio() == null) ? 0 : getProfitPermanmentAssetRatio().hashCode());
        result = prime * result + ((getAdverExpenseIncomeRatio() == null) ? 0 : getAdverExpenseIncomeRatio().hashCode());
        result = prime * result + ((getCustomerEvalueRatio() == null) ? 0 : getCustomerEvalueRatio().hashCode());
        result = prime * result + ((getDeriveToolRiskRatio() == null) ? 0 : getDeriveToolRiskRatio().hashCode());
        result = prime * result + ((getLatestPrice() == null) ? 0 : getLatestPrice().hashCode());
        result = prime * result + ((getTotalPrice() == null) ? 0 : getTotalPrice().hashCode());
        result = prime * result + ((getEarningPerPart() == null) ? 0 : getEarningPerPart().hashCode());
        result = prime * result + ((getAssetPerPart() == null) ? 0 : getAssetPerPart().hashCode());
        result = prime * result + ((getCashFlowPerPart() == null) ? 0 : getCashFlowPerPart().hashCode());
        result = prime * result + ((getTotalPriceFreeCashFlow() == null) ? 0 : getTotalPriceFreeCashFlow().hashCode());
        result = prime * result + ((getEvFreeCaseFlow() == null) ? 0 : getEvFreeCaseFlow().hashCode());
        result = prime * result + ((getEvEbitda() == null) ? 0 : getEvEbitda().hashCode());
        result = prime * result + ((getPriceRatio() == null) ? 0 : getPriceRatio().hashCode());
        result = prime * result + ((getFinalRatio() == null) ? 0 : getFinalRatio().hashCode());
        result = prime * result + ((getTtm() == null) ? 0 : getTtm().hashCode());
        result = prime * result + ((getStaticPeg() == null) ? 0 : getStaticPeg().hashCode());
        result = prime * result + ((getDynamicPeg() == null) ? 0 : getDynamicPeg().hashCode());
        result = prime * result + ((getAvrIncreaseRatioYear() == null) ? 0 : getAvrIncreaseRatioYear().hashCode());
        result = prime * result + ((getTime() == null) ? 0 : getTime().hashCode());
        result = prime * result + ((getUpdateTime() == null) ? 0 : getUpdateTime().hashCode());
        result = prime * result + ((getExpectFutureIncreaseRatio() == null) ? 0 : getExpectFutureIncreaseRatio().hashCode());
        return result;
    }
}