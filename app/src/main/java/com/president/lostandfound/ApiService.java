package com.president.lostandfound;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Query;
import retrofit2.http.Path;
import retrofit2.http.POST;
import retrofit2.http.PUT;

import java.util.concurrent.TimeUnit;
import java.util.List;

public class ApiService {
    private static final String BASE_URL = "http://webprog2.f-host.site/";
    private static ApiInterface apiInterface;

    public interface ApiInterface {
        @POST("api/register")
        Call<RegisterResponse> register(@Body RegisterRequest request);

        @POST("api/login")
        Call<LoginResponse> login(@Body LoginRequest request);

        @GET("api/profile")
        Call<User> getProfile(@Header("Authorization") String token);

        @PUT("api/profile")
        Call<UpdateProfileResponse> updateProfile(@Header("Authorization") String token, @Body UpdateProfileRequest request);

        @GET("api/meetup-points")
        Call<List<MeetupPoint>> getMeetupPoints(@Header("Authorization") String token);

        @POST("api/reports")
        Call<CreateReportResponse> createReport(@Header("Authorization") String token, @Body CreateReportRequest request);

        @GET("api/reports")
        Call<List<Report>> getReports(@Header("Authorization") String token);

        @GET("api/reports/{id}")
        Call<Report> getReportDetail(@Header("Authorization") String token, @Path("id") int reportId);

        @GET("api/reports/{id}/comments")
        Call<List<Comment>> getComments(@Header("Authorization") String token, @Path("id") int reportId);

        @POST("api/reports/{id}/comments")
        Call<AddCommentResponse> addComment(@Header("Authorization") String token, @Path("id") int reportId, @Body AddCommentRequest request);

        @GET("api/reports/search")
        Call<List<Report>> searchReports(@Header("Authorization") String token, @Query("q") String query);
    }

    public static class RegisterRequest {
        String name;
        String email;
        String password;

        public RegisterRequest(String name, String email, String password) {
            this.name = name;
            this.email = email;
            this.password = password;
        }
    }

    public static class LoginRequest {
        String email;
        String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class RegisterResponse {
        String message;
        int userId;
    }

    public static class LoginResponse {
        String message;
        String token;
        User user;
    }

    public static class User {
        int id;
        String name;
        String email;
        String profile_picture;
    }

    public static class UpdateProfileRequest {
        String name;
        String profile_picture;

        public UpdateProfileRequest(String name, String profile_picture) {
            this.name = name;
            this.profile_picture = profile_picture;
        }
    }

    public static class UpdateProfileResponse {
        String message;
        User user;
    }

    public static class MeetupPoint {
        public int id;
        public String name;
        public String location;
        public String created_at;
    }

    public static class CreateReportRequest {
        String title;
        String description;
        String image;
        int meetup_point_id;

        public CreateReportRequest(String title, String description, String image, int meetup_point_id) {
            this.title = title;
            this.description = description;
            this.image = image;
            this.meetup_point_id = meetup_point_id;
        }
    }

    public static class Report {
        public int id;
        public int user_id;
        public String title;
        public String description;
        public String image;
        public int meetup_point_id;
        public String created_at;
        public String updated_at;
        public String user_name;
        public String user_profile_picture;
        public String meetup_point_name;
        public String meetup_point_location;
    }

    public static class CreateReportResponse {
        public String message;
        public Report report;
    }

    public static class Comment {
        public int id;
        public int report_id;
        public int user_id;
        public String comment;
        public String created_at;
        public String updated_at;
        public String user_name;
        public String user_profile_picture;
    }

    public static class AddCommentRequest {
        String comment;

        public AddCommentRequest(String comment) {
            this.comment = comment;
        }
    }

    public static class AddCommentResponse {
        public String message;
        public Comment comment;
    }

    public static ApiInterface getApiService() {
        if (apiInterface == null) {
            HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(interceptor)
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiInterface = retrofit.create(ApiInterface.class);
        }
        return apiInterface;
    }
}