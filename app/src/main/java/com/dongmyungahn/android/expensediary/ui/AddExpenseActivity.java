package com.dongmyungahn.android.expensediary.ui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import com.dongmyungahn.android.expensediary.BuildConfig;
import com.dongmyungahn.android.expensediary.R;
import com.dongmyungahn.android.expensediary.model.Expense;
import com.dongmyungahn.android.expensediary.utilities.ExpenseUtil;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import faranjit.currency.edittext.CurrencyEditText;

import static com.dongmyungahn.android.expensediary.utilities.ExpenseUtil.timestampToCalendar;

public class AddExpenseActivity extends AppCompatActivity {

//    @BindView(R.id.et_expense_amount) EditText mEtExpenseAmount;
    // Used 3rd Party Library
    @BindView(R.id.et_expense_amount) CurrencyEditText mEtExpenseAmount;
    @BindView(R.id.et_expense_description) EditText mEtExpenseDescription;
    @BindView(R.id.et_expense_date) EditText mEtExpenseDate;
    @BindView(R.id.et_expense_time) EditText mEtExpenseTime;
    @BindView(R.id.btn_save) Button mBtnSave;
    @BindView(R.id.adview_add_expense) AdView mAdView;

    private final String TAG = AddExpenseActivity.class.getSimpleName();
    public static final String EXTRA_EXPENSE = "extra_expense";

    private final int MAX_AMOUNT_LENGTH = 11;
    private final int MAX_DESCRIPTION_LENGTH = 50;

    private Expense mExpense;
    private Calendar mCalendar;

    private String mUserId;
    private String mExpenseId;

    private boolean isRemovable;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mExpenseDatabaseReference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        // Butter Knife Bind
        ButterKnife.bind(this);

        MobileAds.initialize(this, BuildConfig.THE_ADMOB_APP_ID);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        // Setup CurrencyEditText
        mEtExpenseAmount.setLocale(new Locale("en", "US"));
        mEtExpenseAmount.showSymbol(true);

        // Firebase Setup
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();

        mExpenseDatabaseReference = mFirebaseDatabase.getReference().child("expense");


        mUserId = null;
        Intent intent = getIntent();
        if (intent != null) {
            try {
                mExpense = intent.getParcelableExtra(EXTRA_EXPENSE);
                if(mExpense == null) {
                    mExpenseId = null;
                }
                else {
                    mExpenseId = mExpense.getExpenseId();
                    if(mExpense.getExpenseDate() > 0) mCalendar = timestampToCalendar(mExpense.getExpenseDate());
                    isRemovable = true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Intent Exception:" + e.getMessage());
                finish();
            }

        } else {
            mExpenseId = null;
        }

        if(mExpenseId == null) {
            // Adding Mode
            mExpense = new Expense();

            mCalendar = Calendar.getInstance();

            isRemovable = false;
        }

        update_display_expense();

        // Set length filter
        mEtExpenseAmount.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_AMOUNT_LENGTH)});
        mEtExpenseDescription.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_DESCRIPTION_LENGTH)});

        // Date Picker Setup
        mEtExpenseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new DatePickerDialog(AddExpenseActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        mCalendar.set(Calendar.YEAR, year);
                        mCalendar.set(Calendar.MONTH, month);
                        mCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        update_display_date();
                    }
                }, mCalendar.get(Calendar.YEAR), mCalendar.get(Calendar.MONTH), mCalendar.get(Calendar.DAY_OF_MONTH))
                        .show();
            }
        });

        // Time Picker Setup
        mEtExpenseTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new TimePickerDialog(AddExpenseActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        mCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        mCalendar.set(Calendar.MINUTE, minute);

                        update_display_time();
                    }
                }, mCalendar.get(Calendar.HOUR_OF_DAY), mCalendar.get(Calendar.MINUTE), false)
                    .show();
            }
        });

        // Save Button Click
        mBtnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Check if all data is valid
                String description = mEtExpenseDescription.getText().toString();
                Double amount = ExpenseUtil.getDoubleFromCurrency(mEtExpenseAmount.getText().toString());
                double expenseDate = mCalendar.getTimeInMillis();

                if( description == null || description.isEmpty() ) {
                    Toast.makeText(AddExpenseActivity.this, getString(R.string.msg_empty_description), Toast.LENGTH_SHORT).show();
                    return;
                }
                if( amount <= 0) {
                    Toast.makeText(AddExpenseActivity.this, getString(R.string.msg_not_valid_amount), Toast.LENGTH_SHORT).show();
                    return;
                }
                // No need to check date because they're handle with picker dialog

                // get expense id from firebase database, if mode is new mode
                if(mExpense.getExpenseId() == null) {
                    mExpenseId = mExpenseDatabaseReference.child(mUserId).push().getKey();
                    mExpense.setExpenseId(mExpenseId);
                }

                // Set member Expense
                mExpense.setAmount(amount);
                mExpense.setDescription(description);
                mExpense.setExpenseDate(expenseDate);

                // Save
                Map<String, Object> childUpdates = new HashMap<>();
                childUpdates.put("/" + mUserId + "/" + mExpenseId, mExpense.toMapExcludeId());
                mExpenseDatabaseReference.updateChildren(childUpdates);

                Toast.makeText(AddExpenseActivity.this, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show();
                finish();
            }
        });


        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null) {
                    // User is signed in
                    //Toast.makeText(MainActivity.this, "Signed In!", Toast.LENGTH_SHORT).show();
                    mUserId = user.getUid();
                } else {
                    // User is signed out
                    finish();
                }

            }
        };
    }

    private void update_display_date() {
        if(mCalendar != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
            mEtExpenseDate.setText(sdf.format(mCalendar.getTime()));
        }
    }

    private void update_display_time() {
        if(mCalendar != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aaa", Locale.US);
            mEtExpenseTime.setText(sdf.format(mCalendar.getTime()));
        }
    }

    private void update_display_expense() {
        // Update Amount
        if(mExpense.getAmount() >= 0) {
            mEtExpenseAmount.setText(ExpenseUtil.expenseCurrencyFormatter(mExpense.getAmount()));
        }
        // Update Description
        if(mExpense.getDescription() != null) {
            mEtExpenseDescription.setText(mExpense.getDescription());
        }

        update_display_date();
        update_display_time();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if(isRemovable) {
            getMenuInflater().inflate(R.menu.menu_add_expense, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_delete:
                // item Delete
                if(mExpenseId != null) {
                    mExpenseDatabaseReference.child(mUserId).child(mExpenseId).removeValue();
                    Toast.makeText(this, getString(R.string.msg_signed_in), Toast.LENGTH_SHORT).show();

                    finish();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove AuthStateListener
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Add AuthStateListener
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }
}
