package com.dongmyungahn.android.expensediary.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

public class Expense implements Parcelable {

    private String expenseId;
    private double amount;
    private String description;
    private double expenseDate;

    public Expense() {
    }

    public Expense(String expenseId, double amount, String description, double expenseDate) {
        this.expenseId = expenseId;
        this.amount = amount;
        this.description = description;
        this.expenseDate = expenseDate;
    }

    public Expense(Parcel in) {
        this.expenseId = in.readString();
        this.amount = in.readDouble();
        this.description = in.readString();
        this.expenseDate = in.readDouble();
    }

    public static final Creator<Expense> CREATOR = new Creator<Expense>() {
        @Override
        public Expense createFromParcel(Parcel source) {
            return new Expense(source);
        }

        @Override
        public Expense[] newArray(int size) {
            return new Expense[size];
        }
    };

    public String getExpenseId() {
        return expenseId;
    }

    public void setExpenseId(String expenseId) {
        this.expenseId = expenseId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getExpenseDate() {
        return expenseDate;
    }

    public void setExpenseDate(double expenseDate) {
        this.expenseDate = expenseDate;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(expenseId);
        dest.writeDouble(amount);
        dest.writeString(description);
        dest.writeDouble(expenseDate);
    }

    @Exclude
    public Map<String, Object> toMapExcludeId() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("amount", amount);
        result.put("description", description);
        result.put("expenseDate", expenseDate);
        return result;
    }
}
