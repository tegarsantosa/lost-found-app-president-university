package com.president.lostandfound;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ReportAdapter adapter;
    private List<ApiService.Report> reports = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        tvEmpty = view.findViewById(R.id.tvEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ReportAdapter(reports, report -> {
            Intent intent = new Intent(requireContext(), ReportDetailActivity.class);
            intent.putExtra("report_id", report.id);
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        loadReports();

        return view;
    }

    private void loadReports() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", null);

        ApiService.getApiService().getReports(token).enqueue(new Callback<List<ApiService.Report>>() {
            @Override
            public void onResponse(Call<List<ApiService.Report>> call, Response<List<ApiService.Report>> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    reports.clear();
                    reports.addAll(response.body());
                    adapter.notifyDataSetChanged();

                    if (reports.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load reports", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ApiService.Report>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load reports", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {

        private List<ApiService.Report> reports;
        private OnReportClickListener listener;

        interface OnReportClickListener {
            void onReportClick(ApiService.Report report);
        }

        ReportAdapter(List<ApiService.Report> reports, OnReportClickListener listener) {
            this.reports = reports;
            this.listener = listener;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_report, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ApiService.Report report = reports.get(position);

            holder.tvTitle.setText(report.title);
            holder.tvDescription.setText(report.description);
            holder.tvMeetupPoint.setText(report.meetup_point_name);

            if (report.image != null && !report.image.isEmpty()) {
                try {
                    String base64Image = report.image;
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.split(",")[1];
                    }
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivImage.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.ivImage.setImageResource(android.R.color.darker_gray);
                }
            }

            holder.itemView.setOnClickListener(v -> listener.onReportClick(report));
        }

        @Override
        public int getItemCount() {
            return reports.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivImage;
            TextView tvTitle, tvDescription, tvMeetupPoint;

            ViewHolder(View itemView) {
                super(itemView);
                ivImage = itemView.findViewById(R.id.ivImage);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvDescription = itemView.findViewById(R.id.tvDescription);
                tvMeetupPoint = itemView.findViewById(R.id.tvMeetupPoint);
            }
        }
    }
}