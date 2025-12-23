package com.president.lostandfound;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.Context.MODE_PRIVATE;

public class ProfileFragment extends Fragment {

    private CircleImageView profileImage;
    private ImageView editImageButton;
    private TextInputEditText nameInput, emailInput;
    private MaterialButton saveButton, logoutButton;
    private ProgressBar progressBar;
    private String selectedImageBase64 = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImage = view.findViewById(R.id.profileImage);
        editImageButton = view.findViewById(R.id.editImageButton);
        nameInput = view.findViewById(R.id.nameInput);
        emailInput = view.findViewById(R.id.emailInput);
        saveButton = view.findViewById(R.id.saveButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        progressBar = view.findViewById(R.id.progressBar);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireActivity().getContentResolver(), imageUri);
                            profileImage.setImageBitmap(bitmap);
                            selectedImageBase64 = bitmapToBase64(bitmap);
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        editImageButton.setOnClickListener(v -> openImagePicker());
        saveButton.setOnClickListener(v -> updateProfile());
        logoutButton.setOnClickListener(v -> logout());

        loadProfile();

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] bytes = baos.toByteArray();
        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void loadProfile() {
        setLoading(true);
        String token = getToken();

        ApiService.getApiService().getProfile("Bearer " + token).enqueue(new Callback<ApiService.User>() {
            @Override
            public void onResponse(Call<ApiService.User> call, Response<ApiService.User> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.User user = response.body();
                    nameInput.setText(user.name);
                    emailInput.setText(user.email);
                } else {
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.User> call, Throwable t) {
                setLoading(false);
                Toast.makeText(getContext(), "Connection error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile() {
        String name = nameInput.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);
        String token = getToken();

        ApiService.UpdateProfileRequest request = new ApiService.UpdateProfileRequest(name, selectedImageBase64);
        ApiService.getApiService().updateProfile("Bearer " + token, request)
                .enqueue(new Callback<ApiService.UpdateProfileResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.UpdateProfileResponse> call, Response<ApiService.UpdateProfileResponse> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
                            selectedImageBase64 = null;
                        } else {
                            Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.UpdateProfileResponse> call, Throwable t) {
                        setLoading(false);
                        Toast.makeText(getContext(), "Connection error", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void logout() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private String getToken() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", MODE_PRIVATE);
        return prefs.getString("token", "");
    }

    private void setLoading(boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
            saveButton.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            saveButton.setEnabled(true);
        }
    }
}