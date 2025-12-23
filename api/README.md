# Lost & Found API Documentation

## Base URL
```
http://localhost:3000/api
```

## Authentication
All endpoints except `/register` and `/login` require authentication using JWT token.

**Header Format:**
```
Authorization: Bearer <your_jwt_token>
```

---

## Endpoints

### 1. Register User

**POST** `/register`

Create a new user account.

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "john@president.ac.id",
  "password": "securepassword123"
}
```

**Response (201):**
```json
{
  "message": "User registered successfully",
  "userId": 1
}
```

**Error Responses:**
- `400` - Missing required fields
- `409` - Email already exists
- `500` - Registration failed

---

### 2. Login

**POST** `/login`

Authenticate user and receive JWT token.

**Request Body:**
```json
{
  "email": "john@president.ac.id",
  "password": "securepassword123"
}
```

**Response (200):**
```json
{
  "message": "Login successful",
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": 1,
    "name": "John Doe",
    "email": "john@president.ac.id",
    "profile_picture": "data:image/jpeg;base64,..."
  }
}
```

**Error Responses:**
- `400` - Missing email or password
- `401` - Invalid credentials
- `500` - Login failed

---

### 3. Get Profile

**GET** `/profile`

Get current user's profile information.

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200):**
```json
{
  "id": 1,
  "name": "John Doe",
  "email": "john@president.ac.id",
  "profile_picture": "data:image/jpeg;base64,...",
  "created_at": "2024-01-15T10:30:00.000Z"
}
```

**Error Responses:**
- `401` - Access token required
- `403` - Invalid or expired token
- `404` - User not found
- `500` - Failed to fetch profile

---

### 4. Update Profile

**PUT** `/profile`

Update user profile (name and/or profile picture).

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "name": "John Smith",
  "profile_picture": "data:image/jpeg;base64,/9j/4AAQSkZJRg..."
}
```

**Note:** Profile picture will be automatically compressed to max 500KB.

**Response (200):**
```json
{
  "message": "Profile updated successfully",
  "user": {
    "id": 1,
    "name": "John Smith",
    "email": "john@president.ac.id",
    "profile_picture": "data:image/jpeg;base64,..."
  }
}
```

**Error Responses:**
- `400` - No fields to update
- `401` - Access token required
- `403` - Invalid or expired token
- `500` - Failed to update profile

---

### 5. Get All Reports

**GET** `/reports`

Get all lost and found reports (latest first).

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200):**
```json
[
  {
    "id": 1,
    "user_id": 1,
    "title": "Lost Blue Backpack",
    "description": "Blue Adidas backpack with laptop inside",
    "image": "data:image/jpeg;base64,...",
    "meetup_point_id": 1,
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "user_name": "John Doe",
    "user_profile_picture": "data:image/jpeg;base64,...",
    "meetup_point_name": "Main Gate",
    "meetup_point_location": "President University Main Entrance"
  }
]
```

**Error Responses:**
- `401` - Access token required
- `403` - Invalid or expired token
- `500` - Failed to fetch reports

---

### 6. Search Reports

**GET** `/reports/search?q=<search_query>`

Search reports by title or description.

**Headers:**
```
Authorization: Bearer <token>
```

**Query Parameters:**
- `q` (required) - Search query string

**Example:**
```
GET /reports/search?q=backpack
```

**Response (200):**
```json
[
  {
    "id": 1,
    "user_id": 1,
    "title": "Lost Blue Backpack",
    "description": "Blue Adidas backpack with laptop inside",
    "image": "data:image/jpeg;base64,...",
    "meetup_point_id": 1,
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "user_name": "John Doe",
    "user_profile_picture": "data:image/jpeg;base64,...",
    "meetup_point_name": "Main Gate",
    "meetup_point_location": "President University Main Entrance"
  }
]
```

**Error Responses:**
- `400` - Search query required
- `401` - Access token required
- `403` - Invalid or expired token
- `500` - Search failed

---

### 7. Get Report Details

**GET** `/reports/:id`

Get detailed information about a specific report.

**Headers:**
```
Authorization: Bearer <token>
```

**Example:**
```
GET /reports/1
```

**Response (200):**
```json
{
  "id": 1,
  "user_id": 1,
  "title": "Lost Blue Backpack",
  "description": "Blue Adidas backpack with laptop inside",
  "image": "data:image/jpeg;base64,...",
  "meetup_point_id": 1,
  "created_at": "2024-01-15T10:30:00.000Z",
  "updated_at": "2024-01-15T10:30:00.000Z",
  "user_name": "John Doe",
  "user_profile_picture": "data:image/jpeg;base64,...",
  "meetup_point_name": "Main Gate",
  "meetup_point_location": "President University Main Entrance"
}
```

**Error Responses:**
- `401` - Access token required
- `403` - Invalid or expired token
- `404` - Report not found
- `500` - Failed to fetch report

---

### 8. Create Report

**POST** `/reports`

Create a new lost and found report.

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "title": "Lost Blue Backpack",
  "description": "Blue Adidas backpack with laptop inside, found near library",
  "image": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "meetup_point_id": 1
}
```

**Note:** Image will be automatically compressed to max 500KB.

**Response (201):**
```json
{
  "message": "Report created successfully",
  "report": {
    "id": 1,
    "user_id": 1,
    "title": "Lost Blue Backpack",
    "description": "Blue Adidas backpack with laptop inside, found near library",
    "image": "data:image/jpeg;base64,...",
    "meetup_point_id": 1,
    "created_at": "2024-01-15T10:30:00.000Z",
    "updated_at": "2024-01-15T10:30:00.000Z",
    "user_name": "John Doe",
    "user_profile_picture": "data:image/jpeg;base64,...",
    "meetup_point_name": "Main Gate",
    "meetup_point_location": "President University Main Entrance"
  }
}
```

**Error Responses:**
- `400` - Missing required fields
- `401` - Access token required
- `403` - Invalid or expired token
- `500` - Failed to create report

---

### 9. Get Meetup Points

**GET** `/meetup-points`

Get all available meetup points.

**Headers:**
```
Authorization: Bearer <token>
```

**Response (200):**
```json
[
  {
    "id": 1,
    "name": "Main Gate",
    "location": "President University Main Entrance",
    "created_at": "2024-01-15T10:30:00.000Z"
  },
  {
    "id": 2,
    "name": "Library",
    "location": "President University Library Building",
    "created_at": "2024-01-15T10:30:00.000Z"
  }
]
```

**Error Responses:**
- `401` - Access token required
- `403` - Invalid or expired token
- `500` - Failed to fetch meetup points

---

### 10. Get Report Comments

**GET** `/reports/:id/comments`

Get all comments for a specific report.

**Headers:**
```
Authorization: Bearer <token>
```

**Example:**
```
GET /reports/1/comments
```

**Response (200):**
```json
[
  {
    "id": 1,
    "report_id": 1,
    "user_id": 2,
    "comment": "I think I saw this backpack near the cafeteria yesterday!",
    "created_at": "2024-01-15T11:00:00.000Z",
    "updated_at": "2024-01-15T11:00:00.000Z",
    "user_name": "Jane Smith",
    "user_profile_picture": "data:image/jpeg;base64,..."
  }
]
```

**Error Responses:**
- `401` - Access token required
- `403` - Invalid or expired token
- `500` - Failed to fetch comments

---

### 11. Add Comment to Report

**POST** `/reports/:id/comments`

Add a comment to a specific report.

**Headers:**
```
Authorization: Bearer <token>
```

**Request Body:**
```json
{
  "comment": "I think I saw this backpack near the cafeteria yesterday!"
}
```

**Response (201):**
```json
{
  "message": "Comment added successfully",
  "comment": {
    "id": 1,
    "report_id": 1,
    "user_id": 2,
    "comment": "I think I saw this backpack near the cafeteria yesterday!",
    "created_at": "2024-01-15T11:00:00.000Z",
    "updated_at": "2024-01-15T11:00:00.000Z",
    "user_name": "Jane Smith",
    "user_profile_picture": "data:image/jpeg;base64,..."
  }
}
```

**Error Responses:**
- `400` - Comment is required
- `401` - Access token required
- `403` - Invalid or expired token
- `500` - Failed to add comment

---

## Image Format

All images (profile pictures and report images) should be sent as Base64 encoded strings with the data URI format:

```
data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD...
```

Images will be automatically compressed to a maximum of 500KB by the server.

---

## Installation & Setup

1. Install dependencies:
```bash
npm install
```

2. Configure MySQL database connection in `server.js`:
```javascript
const pool = mysql.createPool({
  host: 'localhost',
  user: 'root',
  password: 'your_password',
  database: 'lost_found_db'
});
```

3. Create database and tables:
```bash
mysql -u root -p < schema.sql
```

4. Change JWT secret in `server.js`:
```javascript
const JWT_SECRET = 'your-secret-key-change-this';
```

5. Run the server:
```bash
npm start
```

Or for development with auto-reload:
```bash
npm run dev
```

---

## Error Response Format

All errors follow this format:

```json
{
  "error": "Error message description"
}
```

---

## Notes

- JWT tokens expire after 30 days
- All timestamps are in ISO 8601 format
- Images are automatically compressed to max 500KB
- Search is case-insensitive
- Comments are ordered chronologically (oldest first)
- Reports are ordered by creation date (newest first)