package com.dongmyungahn.android.expensediary.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.dongmyungahn.android.expensediary.R;
import com.dongmyungahn.android.expensediary.model.Expense;
import com.dongmyungahn.android.expensediary.utilities.ExpenseUtil;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.Arrays;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.rv_expense_list) RecyclerView mRvExpenseList;
    @BindView(R.id.fab_add) FloatingActionButton mFabAdd;


    public static final int RC_SIGN_IN = 1; // For Firebase Auth

    private final String TAG = MainActivity.class.getSimpleName();

    // Firebase
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mExpenseDatabaseReference;
    private ChildEventListener mChildEventListener;
    private Query mExpenseOrderByDateQuery;

    private String mUserId;

    private FirebaseRecyclerAdapter mExpenseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Butter Knife Bind
        ButterKnife.bind(this);

        // Firebase Setup
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mExpenseDatabaseReference = mFirebaseDatabase.getReference().child("expense");

        // Layout Manager
        LinearLayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        layoutManager.setReverseLayout(true); // for recent data goes to top
        layoutManager.setStackFromEnd(true);  // for recent data goes to top
        mRvExpenseList.setLayoutManager(layoutManager);

        // Add divider
        DividerItemDecoration divider = new DividerItemDecoration(getApplicationContext(), layoutManager.getOrientation());
        mRvExpenseList.addItemDecoration(divider);

        // FAB click
        mFabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent addExpenseIntent = new Intent(MainActivity.this, AddExpenseActivity.class);
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    // For Transition
                    startActivity(addExpenseIntent, ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                } else {
                    startActivity(addExpenseIntent);
                }
            }
        });

        // Auth State Listener
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                login_process(firebaseAuth);
            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_sign_out:
                AuthUI.getInstance()
                        .signOut(this);
                // After sign out, finish program
                finish();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN) {
            if(resultCode == RESULT_OK) {
                Toast.makeText(this, getString(R.string.msg_signed_in), Toast.LENGTH_SHORT).show();
                mUserId = mFirebaseAuth.getCurrentUser().getUid();

            } else if(resultCode == RESULT_CANCELED) {
                Toast.makeText(this, getString(R.string.msg_sign_in_canceled), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove AuthStateListener
        if(mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Add AuthStateListener
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mExpenseAdapter != null)  mExpenseAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mExpenseAdapter != null)  mExpenseAdapter.stopListening();
    }

    private void login_process(FirebaseAuth firebaseAuth) {
        if(firebaseAuth != null) {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if(user != null) {
                mUserId = user.getUid();
                setupAdapter();
            } else {
                mUserId = null;
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false)
                                .setAvailableProviders(Arrays.asList(
                                        new AuthUI.IdpConfig.EmailBuilder().build()))
                                .build(),
                        RC_SIGN_IN);
            }
        }
    }

    private void setupAdapter() {
        if(mUserId != null) {
            mExpenseOrderByDateQuery = mExpenseDatabaseReference.child(mUserId).orderByChild("expenseDate");

            FirebaseRecyclerOptions<Expense> options =
                    new FirebaseRecyclerOptions.Builder<Expense>()
                            .setQuery(mExpenseOrderByDateQuery, new SnapshotParser<Expense>() {
                                @NonNull
                                @Override
                                public Expense parseSnapshot(@NonNull DataSnapshot snapshot) {
                                    return new Expense(snapshot.getKey().toString(),
                                            Double.parseDouble(snapshot.child("amount").getValue().toString()),
                                            snapshot.child("description").getValue().toString(),
                                            Double.parseDouble(snapshot.child("expenseDate").getValue().toString()));
                                }
                            })
                            .build();

            mExpenseAdapter = new FirebaseRecyclerAdapter<Expense, ExpenseViewHolder>(options) {
                @NonNull
                @Override
                public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
                    View view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.expense_list_item, parent, false);
                    return new ExpenseViewHolder(view);
                }

                @Override
                protected void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position, @NonNull final Expense expense) {
                    // Bind Data
                    holder.mTvExpenseAmount.setText(ExpenseUtil.expenseCurrencyFormatter(expense.getAmount()));
                    holder.mTvExpenseDescription.setText(expense.getDescription());
                    holder.mTvExpenseDate.setText(ExpenseUtil.timestampToDateString(expense.getExpenseDate()));
                    // For Accessibility
                    holder.mTvExpenseAmount.setContentDescription(ExpenseUtil.expenseCurrencyFormatter(expense.getAmount()));
                    holder.mTvExpenseDescription.setContentDescription(expense.getDescription());
                    holder.mTvExpenseDate.setContentDescription(ExpenseUtil.timestampToDateString(expense.getExpenseDate()));

                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent updateIntent = new Intent(MainActivity.this, AddExpenseActivity.class);
                            updateIntent.putExtra(AddExpenseActivity.EXTRA_EXPENSE, expense);
                            if (android.os.Build.VERSION.SDK_INT >= 21) {
                                // For Transition
                                startActivity(updateIntent, ActivityOptions.makeSceneTransitionAnimation(MainActivity.this).toBundle());
                            } else {
                                startActivity(updateIntent);
                            }
                        }
                    });
                }
            };
            mExpenseAdapter.notifyDataSetChanged();
            mRvExpenseList.setAdapter(mExpenseAdapter);
            mExpenseAdapter.startListening();
        }
    }

    // Sub Class
    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.tv_expense_amount) TextView mTvExpenseAmount;
        @BindView(R.id.tv_expense_description) TextView mTvExpenseDescription;
        @BindView(R.id.tv_expense_date) TextView mTvExpenseDate;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
