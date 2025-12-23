package com.president.lostandfound;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateFragment extends Fragment {

    private EditText etTitle, etDescription;
    private ImageView ivPreview;
    private Spinner spinnerMeetupPoint;
    private Button btnSelectImage, btnSubmit;
    private ProgressBar progressBar;

    private String base64Image = null;
    private List<ApiService.MeetupPoint> meetupPoints = new ArrayList<>();

    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        handleImageSelection(imageUri);
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_create, container, false);

        etTitle = view.findViewById(R.id.etTitle);
        etDescription = view.findViewById(R.id.etDescription);
        ivPreview = view.findViewById(R.id.ivPreview);
        spinnerMeetupPoint = view.findViewById(R.id.spinnerMeetupPoint);
        btnSelectImage = view.findViewById(R.id.btnSelectImage);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        progressBar = view.findViewById(R.id.progressBar);

        btnSelectImage.setOnClickListener(v -> selectImage());
        btnSubmit.setOnClickListener(v -> createReport());

        loadMeetupPoints();

        return view;
    }

    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void handleImageSelection(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            Bitmap resizedBitmap = resizeBitmap(bitmap, 800, 800);

            ivPreview.setImageBitmap(resizedBitmap);
            ivPreview.setVisibility(View.VISIBLE);

            base64Image = bitmapToBase64(resizedBitmap);

        } catch (IOException e) {
            Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float ratioBitmap = (float) width / (float) height;
        float ratioMax = (float) maxWidth / (float) maxHeight;

        int finalWidth = maxWidth;
        int finalHeight = maxHeight;

        if (ratioMax > ratioBitmap) {
            finalWidth = (int) ((float) maxHeight * ratioBitmap);
        } else {
            finalHeight = (int) ((float) maxWidth / ratioBitmap);
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP);
    }

    private void loadMeetupPoints() {
        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", null);

        ApiService.getApiService().getMeetupPoints(token).enqueue(new Callback<List<ApiService.MeetupPoint>>() {
            @Override
            public void onResponse(Call<List<ApiService.MeetupPoint>> call, Response<List<ApiService.MeetupPoint>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    meetupPoints = response.body();
                    setupSpinner();
                } else {
                    Toast.makeText(requireContext(), "Failed to load meetup points", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<ApiService.MeetupPoint>> call, Throwable t) {
                Toast.makeText(requireContext(), "Failed to load meetup points", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinner() {
        List<String> pointNames = new ArrayList<>();
        for (ApiService.MeetupPoint point : meetupPoints) {
            pointNames.add(point.name + " - " + point.location);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                pointNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMeetupPoint.setAdapter(adapter);
    }

    private void createReport() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a description", Toast.LENGTH_SHORT).show();
            return;
        }

        if (base64Image == null) {
            Toast.makeText(requireContext(), "Please select an image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (meetupPoints.isEmpty()) {
            Toast.makeText(requireContext(), "No meetup points available", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPosition = spinnerMeetupPoint.getSelectedItemPosition();
        int meetupPointId = meetupPoints.get(selectedPosition).id;

        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        SharedPreferences prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        String token = "Bearer " + prefs.getString("token", null);

        ApiService.CreateReportRequest request = new ApiService.CreateReportRequest(
                title, description, base64Image, meetupPointId
        );

        ApiService.getApiService().createReport(token, request).enqueue(new Callback<ApiService.CreateReportResponse>() {
            @Override
            public void onResponse(Call<ApiService.CreateReportResponse> call, Response<ApiService.CreateReportResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Report created successfully!", Toast.LENGTH_SHORT).show();
                    resetForm();
                } else {
                    Toast.makeText(requireContext(), "Failed to create report", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.CreateReportResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnSubmit.setEnabled(true);
                Toast.makeText(requireContext(), "Failed to create report", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetForm() {
        etTitle.setText("");
        etDescription.setText("");
        ivPreview.setVisibility(View.GONE);
        base64Image = null;
        if (spinnerMeetupPoint.getAdapter() != null && spinnerMeetupPoint.getAdapter().getCount() > 0) {
            spinnerMeetupPoint.setSelection(0);
        }
    }
}