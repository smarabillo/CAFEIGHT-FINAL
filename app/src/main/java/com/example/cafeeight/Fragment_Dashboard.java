package com.example.cafeeight;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Fragment_Dashboard extends Fragment {

    private static final int MAX_Y_VALUE = 20000;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        GraphView graphView = view.findViewById(R.id.idGraphView);

        List<Order> dailyOrders = getDailySalesData();

        LineGraphSeries<DataPoint> series = new LineGraphSeries<>(getDataPoints(dailyOrders));

        graphView.setTitleColor(getResources().getColor(R.color.black));
        graphView.setTitleTextSize(24);

        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(0);
        graphView.getViewport().setMaxY(MAX_Y_VALUE);

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graphView);
        staticLabelsFormatter.setHorizontalLabels(new String[]{"","Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"});
        graphView.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);

        graphView.addSeries(series);
        updateGraph();
        return view;
    }

    private void updateGraph() {
        if (getView() != null) {
            GraphView graphView = getView().findViewById(R.id.idGraphView);

            if (graphView != null) {
                List<Order> updatedData = new DatabaseHelper(requireContext()).getUpdatedData();

                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(getDataPoints(updatedData));
                series.setDrawDataPoints(false);
                series.setColor(Color.RED);
                series.setThickness(4);

                graphView.removeAllSeries();graphView.addSeries(series);
            } else {
                Log.e("Fragment_Dashboard", "GraphView is null");
            }
        } else {
            Log.e("Fragment_Dashboard", "Fragment view is null");
        }
    }


    private List<Order> getDailySalesData() {
        return new DatabaseHelper(requireContext()).getTodaySalesData();
    }

    private DataPoint[] getDataPoints(List<Order> dailyOrders) {

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        DataPoint[] dataPoints = new DataPoint[7];
        for (int i = 0; i < 7; i++) {
            double saleValue = 0;
            for (Order order : dailyOrders) {
                if (isSameDay(order.getOrderDate(), getDateString(calendar))) {
                    saleValue += order.getTotalAmount();
                }
            }
            dataPoints[i] = new DataPoint(i, saleValue);
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        return dataPoints;
    }

    private boolean isSameDay(String dateString1, String dateString2) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        try {
            Date date1 = dateFormat.parse(dateString1);
            Date date2 = dateFormat.parse(dateString2);

            if (date1 != null && date2 != null) {
                Calendar cal1 = Calendar.getInstance();
                Calendar cal2 = Calendar.getInstance();
                cal1.setTime(date1);
                cal2.setTime(date2);


                return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
                        && cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH)
                        && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH);
            }
        } catch (ParseException e) {

            Log.e("Date Parsing Error", "Error parsing date", e);
        }

        return false;
    }

    private String getDateString(Calendar calendar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return dateFormat.format(calendar.getTime());
    }

}
