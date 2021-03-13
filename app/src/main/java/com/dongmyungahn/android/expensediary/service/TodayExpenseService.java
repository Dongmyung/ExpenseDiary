package com.dongmyungahn.android.expensediary.service;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.dongmyungahn.android.expensediary.widget.ExpenseDiaryAppWidgetProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

public class TodayExpenseService extends IntentService {

    public static final String ACTION_UPDATE_WIDGET = "com.dongmyungahn.android.expensediary.action.update_widget";

    private final String TAG = TodayExpenseService.class.getSimpleName();

    public TodayExpenseService() {
        super("TodayExpenseService");
    }

    public static void startActionUpdateWidgets(Context context) {
        Intent intent = new Intent(context, TodayExpenseService.class);
        intent.setAction(ACTION_UPDATE_WIDGET);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        final String action = intent.getAction();
        if (ACTION_UPDATE_WIDGET.equals(action)) {
            handleActionUpdateWidget();
        }
    }

    private void handleActionUpdateWidget() {

        // Firebase auth check
        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth)  {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {

                    // Get today's timestamp
                    Calendar calendar = Calendar.getInstance();
                    // Reset time to 00:00:00
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    double timestamp_today = calendar.getTimeInMillis();
                    // Get tomorrow's timestamp
                    calendar.add(Calendar.DATE,1);
                    double timestamp_tomorrow = calendar.getTimeInMillis();

                    // Firebase Database
                    // timestamp_tomorrow -1 => because endAt means equal or less than.
                    Query query = FirebaseDatabase.getInstance().getReference().child("expense").child(user.getUid())
                                            .orderByChild("expenseDate").startAt(timestamp_today).endAt(timestamp_tomorrow-1);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            double today_expense = 0;
                            for( DataSnapshot ds :dataSnapshot.getChildren()) {
                                today_expense += Double.parseDouble(ds.child("amount").getValue().toString());
                            }

                            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(TodayExpenseService.this);
                            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(TodayExpenseService.this, ExpenseDiaryAppWidgetProvider.class));
                            //Now update all widgets
                            ExpenseDiaryAppWidgetProvider.updateAppWidgets(TodayExpenseService.this, appWidgetManager, today_expense, appWidgetIds);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                }
            }
        });
    }
}
