package com.dongmyungahn.android.expensediary.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.dongmyungahn.android.expensediary.R;
import com.dongmyungahn.android.expensediary.service.TodayExpenseService;
import com.dongmyungahn.android.expensediary.ui.MainActivity;
import com.dongmyungahn.android.expensediary.utilities.ExpenseUtil;



public class ExpenseDiaryAppWidgetProvider extends AppWidgetProvider {

    private final static String TAG = ExpenseDiaryAppWidgetProvider.class.getSimpleName();

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, double amount, int appWidgetId) {
        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.expense_diary_app_widget);
        views.setTextViewText(R.id.tv_widget_today_amount, ExpenseUtil.expenseCurrencyFormatter(amount));
        views.setContentDescription(R.id.tv_widget_today_amount, ExpenseUtil.expenseCurrencyFormatter(amount));

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.tv_widget_today_amount, pendingIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }


    public static void updateAppWidgets(Context context, AppWidgetManager appWidgetManager, double amount, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, amount, appWidgetId);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        TodayExpenseService.startActionUpdateWidgets(context);
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }
}

