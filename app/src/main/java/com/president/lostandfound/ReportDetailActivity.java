package com.president.lostandfound;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportDetailActivity extends AppCompatActivity {

    private ImageView ivBack, ivImage, ivUserProfile;
    private TextView tvTitle, tvDescription, tvUserName, tvMeetupPoint, tvMeetupLocation, tvDate;
    private RecyclerView recyclerViewComments;
    private EditText etComment;
    private Button btnSendComment;
    private ProgressBar progressBar, progressBarComments;

    private int reportId;
    private CommentAdapter commentAdapter;
    private List<ApiService.Comment> comments = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_detail);

        reportId = getIntent().getIntExtra("report_id", -1);

        ivBack = findViewById(R.id.ivBack);
        ivImage = findViewById(R.id.ivImage);
        ivUserProfile = findViewById(R.id.ivUserProfile);
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvUserName = findViewById(R.id.tvUserName);
        tvMeetupPoint = findViewById(R.id.tvMeetupPoint);
        tvMeetupLocation = findViewById(R.id.tvMeetupLocation);
        tvDate = findViewById(R.id.tvDate);
        recyclerViewComments = findViewById(R.id.recyclerViewComments);
        etComment = findViewById(R.id.etComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        progressBar = findViewById(R.id.progressBar);
        progressBarComments = findViewById(R.id.progressBarComments);

        ivBack.setOnClickListener(v -> finish());
        btnSendComment.setOnClickListener(v -> addComment());

        recyclerViewComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter(comments);
        recyclerViewComments.setAdapter(commentAdapter);

        loadReportDetails();
        loadComments();
    }

    private void loadReportDetails() {
        progressBar.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", null);

        ApiService.getApiService().getReportDetail(token, reportId).enqueue(new Callback<ApiService.Report>() {
            @Override
            public void onResponse(Call<ApiService.Report> call, Response<ApiService.Report> response) {
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    displayReport(response.body());
                } else {
                    Toast.makeText(ReportDetailActivity.this, "Failed to load report", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.Report> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ReportDetailActivity.this, "Failed to load report", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayReport(ApiService.Report report) {
        tvTitle.setText(report.title);
        tvDescription.setText(report.description);
        tvUserName.setText(report.user_name);
        tvMeetupPoint.setText(report.meetup_point_name);
        tvMeetupLocation.setText(report.meetup_point_location);
        tvDate.setText(formatDate(report.created_at));

        if (report.image != null && !report.image.isEmpty()) {
            loadImage(report.image, ivImage);
        }

        if (report.user_profile_picture != null && !report.user_profile_picture.isEmpty()) {
            loadImage(report.user_profile_picture, ivUserProfile);
        }
    }

    private void loadImage(String base64Image, ImageView imageView) {
        try {
            if (base64Image.contains(",")) {
                base64Image = base64Image.split(",")[1];
            }
            byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            imageView.setImageBitmap(decodedByte);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadComments() {
        progressBarComments.setVisibility(View.VISIBLE);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", null);

        ApiService.getApiService().getComments(token, reportId).enqueue(new Callback<List<ApiService.Comment>>() {
            @Override
            public void onResponse(Call<List<ApiService.Comment>> call, Response<List<ApiService.Comment>> response) {
                progressBarComments.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    comments.clear();
                    comments.addAll(response.body());
                    commentAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<ApiService.Comment>> call, Throwable t) {
                progressBarComments.setVisibility(View.GONE);
            }
        });
    }

    private void addComment() {
        String commentText = etComment.getText().toString().trim();

        if (commentText.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendComment.setEnabled(false);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", null);

        ApiService.AddCommentRequest request = new ApiService.AddCommentRequest(commentText);

        ApiService.getApiService().addComment(token, reportId, request).enqueue(new Callback<ApiService.AddCommentResponse>() {
            @Override
            public void onResponse(Call<ApiService.AddCommentResponse> call, Response<ApiService.AddCommentResponse> response) {
                btnSendComment.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    etComment.setText("");
                    comments.add(response.body().comment);
                    commentAdapter.notifyItemInserted(comments.size() - 1);
                    recyclerViewComments.scrollToPosition(comments.size() - 1);
                    Toast.makeText(ReportDetailActivity.this, "Comment added", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ReportDetailActivity.this, "Failed to add comment", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.AddCommentResponse> call, Throwable t) {
                btnSendComment.setEnabled(true);
                Toast.makeText(ReportDetailActivity.this, "Failed to add comment", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDate(String dateString) {
        try {
            return dateString.substring(0, 10);
        } catch (Exception e) {
            return dateString;
        }
    }

    private static class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.ViewHolder> {

        private List<ApiService.Comment> comments;

        CommentAdapter(List<ApiService.Comment> comments) {
            this.comments = comments;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_comment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            ApiService.Comment comment = comments.get(position);

            holder.tvUserName.setText(comment.user_name);
            holder.tvComment.setText(comment.comment);
            holder.tvDate.setText(formatDate(comment.created_at));

            if (comment.user_profile_picture != null && !comment.user_profile_picture.isEmpty()) {
                try {
                    String base64Image = comment.user_profile_picture;
                    if (base64Image.contains(",")) {
                        base64Image = base64Image.split(",")[1];
                    }
                    byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    holder.ivUserProfile.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    holder.ivUserProfile.setImageResource(android.R.drawable.ic_menu_gallery);
                }
            }
        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private static String formatDate(String dateString) {
            try {
                return dateString.substring(0, 10);
            } catch (Exception e) {
                return dateString;
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivUserProfile;
            TextView tvUserName, tvComment, tvDate;

            ViewHolder(View itemView) {
                super(itemView);
                ivUserProfile = itemView.findViewById(R.id.ivUserProfile);
                tvUserName = itemView.findViewById(R.id.tvUserName);
                tvComment = itemView.findViewById(R.id.tvComment);
                tvDate = itemView.findViewById(R.id.tvDate);
            }
        }
    }
}