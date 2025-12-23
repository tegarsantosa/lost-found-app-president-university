require('dotenv').config();
const express = require('express');
const mysql = require('mysql2/promise');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const multer = require('multer');
const sharp = require('sharp');
const cors = require('cors');

const {
  PORT,
  JWT_SECRET,
  DB_HOST,
  DB_USER,
  DB_PASSWORD,
  DB_NAME,
  DB_CONNECTION_LIMIT,
  JSON_LIMIT
} = process.env;

const app = express();

app.use(cors());
app.use(express.json({ limit: JSON_LIMIT }));

const pool = mysql.createPool({
  host: DB_HOST,
  user: DB_USER,
  password: DB_PASSWORD,
  database: DB_NAME,
  waitForConnections: true,
  connectionLimit: DB_CONNECTION_LIMIT,
  queueLimit: 0
});

const storage = multer.memoryStorage();
const upload = multer({ storage: storage });

const authenticateToken = (req, res, next) => {
  const authHeader = req.headers['authorization'];
  const token = authHeader && authHeader.split(' ')[1];

  if (!token) {
    return res.status(401).json({ error: 'Access token required' });
  }

  jwt.verify(token, JWT_SECRET, (err, user) => {
    if (err) {
      return res.status(403).json({ error: 'Invalid or expired token' });
    }
    req.user = user;
    next();
  });
};

const compressImage = async (base64Image) => {
  const buffer = Buffer.from(base64Image.split(',')[1], 'base64');
  let quality = 90;
  let compressed = await sharp(buffer).jpeg({ quality }).toBuffer();

  while (compressed.length > 500000 && quality > 10) {
    quality -= 10;
    compressed = await sharp(buffer).jpeg({ quality }).toBuffer();
  }

  return `data:image/jpeg;base64,${compressed.toString('base64')}`;
};

app.post('/api/register', async (req, res) => {
  try {
    const { name, email, password } = req.body;

    if (!name || !email || !password) {
      return res.status(400).json({ error: 'All fields are required' });
    }

    const hashedPassword = await bcrypt.hash(password, 10);

    const [result] = await pool.query(
      'INSERT INTO users (name, email, password) VALUES (?, ?, ?)',
      [name, email, hashedPassword]
    );

    res.status(201).json({
      message: 'User registered successfully',
      userId: result.insertId
    });
  } catch (error) {
    if (error.code === 'ER_DUP_ENTRY') {
      return res.status(409).json({ error: 'Email already exists' });
    }
    res.status(500).json({ error: 'Registration failed' });
  }
});

app.post('/api/login', async (req, res) => {
  try {
    const { email, password } = req.body;

    if (!email || !password) {
      return res.status(400).json({ error: 'Email and password required' });
    }

    const [users] = await pool.query('SELECT * FROM users WHERE email = ?', [email]);

    if (users.length === 0) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const user = users[0];
    const validPassword = await bcrypt.compare(password, user.password);

    if (!validPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const token = jwt.sign({ userId: user.id, email: user.email }, JWT_SECRET, {
      expiresIn: '30d'
    });

    res.json({
      message: 'Login successful',
      token,
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        profile_picture: user.profile_picture
      }
    });
  } catch (error) {
    res.status(500).json({ error: 'Login failed' });
  }
});

app.get('/api/profile', authenticateToken, async (req, res) => {
  try {
    const [users] = await pool.query(
      'SELECT id, name, email, profile_picture, created_at FROM users WHERE id = ?',
      [req.user.userId]
    );

    if (users.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json(users[0]);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch profile' });
  }
});

app.put('/api/profile', authenticateToken, async (req, res) => {
  try {
    const { name, profile_picture } = req.body;
    let compressedImage = null;

    if (profile_picture) {
      compressedImage = await compressImage(profile_picture);
    }

    const updateFields = [];
    const updateValues = [];

    if (name) {
      updateFields.push('name = ?');
      updateValues.push(name);
    }

    if (compressedImage) {
      updateFields.push('profile_picture = ?');
      updateValues.push(compressedImage);
    }

    if (updateFields.length === 0) {
      return res.status(400).json({ error: 'No fields to update' });
    }

    updateValues.push(req.user.userId);

    await pool.query(
      `UPDATE users SET ${updateFields.join(', ')} WHERE id = ?`,
      updateValues
    );

    const [users] = await pool.query(
      'SELECT id, name, email, profile_picture FROM users WHERE id = ?',
      [req.user.userId]
    );

    res.json({ message: 'Profile updated successfully', user: users[0] });
  } catch (error) {
    res.status(500).json({ error: 'Failed to update profile' });
  }
});

app.get('/api/reports', authenticateToken, async (req, res) => {
  try {
    const [reports] = await pool.query(
      `SELECT r.*, u.name as user_name, u.profile_picture as user_profile_picture, 
       m.name as meetup_point_name, m.location as meetup_point_location
       FROM reports r
       JOIN users u ON r.user_id = u.id
       JOIN meetup_points m ON r.meetup_point_id = m.id
       ORDER BY r.created_at DESC`
    );

    res.json(reports);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch reports' });
  }
});

app.get('/api/reports/search', authenticateToken, async (req, res) => {
  try {
    const { q } = req.query;

    if (!q) {
      return res.status(400).json({ error: 'Search query required' });
    }

    const [reports] = await pool.query(
      `SELECT r.*, u.name as user_name, u.profile_picture as user_profile_picture,
       m.name as meetup_point_name, m.location as meetup_point_location
       FROM reports r
       JOIN users u ON r.user_id = u.id
       JOIN meetup_points m ON r.meetup_point_id = m.id
       WHERE r.title LIKE ? OR r.description LIKE ?
       ORDER BY r.created_at DESC`,
      [`%${q}%`, `%${q}%`]
    );

    res.json(reports);
  } catch (error) {
    res.status(500).json({ error: 'Search failed' });
  }
});

app.get('/api/reports/:id', authenticateToken, async (req, res) => {
  try {
    const [reports] = await pool.query(
      `SELECT r.*, u.name as user_name, u.profile_picture as user_profile_picture,
       m.name as meetup_point_name, m.location as meetup_point_location
       FROM reports r
       JOIN users u ON r.user_id = u.id
       JOIN meetup_points m ON r.meetup_point_id = m.id
       WHERE r.id = ?`,
      [req.params.id]
    );

    if (reports.length === 0) {
      return res.status(404).json({ error: 'Report not found' });
    }

    res.json(reports[0]);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch report' });
  }
});

app.post('/api/reports', authenticateToken, async (req, res) => {
  try {
    const { title, description, image, meetup_point_id } = req.body;

    if (!title || !description || !image || !meetup_point_id) {
      return res.status(400).json({ error: 'All fields are required' });
    }

    const compressedImage = await compressImage(image);

    const [result] = await pool.query(
      'INSERT INTO reports (user_id, title, description, image, meetup_point_id) VALUES (?, ?, ?, ?, ?)',
      [req.user.userId, title, description, compressedImage, meetup_point_id]
    );

    const [newReport] = await pool.query(
      `SELECT r.*, u.name as user_name, u.profile_picture as user_profile_picture,
       m.name as meetup_point_name, m.location as meetup_point_location
       FROM reports r
       JOIN users u ON r.user_id = u.id
       JOIN meetup_points m ON r.meetup_point_id = m.id
       WHERE r.id = ?`,
      [result.insertId]
    );

    res.status(201).json({
      message: 'Report created successfully',
      report: newReport[0]
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to create report' });
  }
});

app.get('/api/meetup-points', authenticateToken, async (req, res) => {
  try {
    const [points] = await pool.query('SELECT * FROM meetup_points ORDER BY name');
    res.json(points);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch meetup points' });
  }
});

app.get('/api/reports/:id/comments', authenticateToken, async (req, res) => {
  try {
    const [comments] = await pool.query(
      `SELECT c.*, u.name as user_name, u.profile_picture as user_profile_picture
       FROM comments c
       JOIN users u ON c.user_id = u.id
       WHERE c.report_id = ?
       ORDER BY c.created_at ASC`,
      [req.params.id]
    );

    res.json(comments);
  } catch (error) {
    res.status(500).json({ error: 'Failed to fetch comments' });
  }
});

app.post('/api/reports/:id/comments', authenticateToken, async (req, res) => {
  try {
    const { comment } = req.body;

    if (!comment) {
      return res.status(400).json({ error: 'Comment is required' });
    }

    const [result] = await pool.query(
      'INSERT INTO comments (report_id, user_id, comment) VALUES (?, ?, ?)',
      [req.params.id, req.user.userId, comment]
    );

    const [newComment] = await pool.query(
      `SELECT c.*, u.name as user_name, u.profile_picture as user_profile_picture
       FROM comments c
       JOIN users u ON c.user_id = u.id
       WHERE c.id = ?`,
      [result.insertId]
    );

    res.status(201).json({
      message: 'Comment added successfully',
      comment: newComment[0]
    });
  } catch (error) {
    res.status(500).json({ error: 'Failed to add comment' });
  }
});

app.listen(PORT, () => {
  console.log(`Server running on port ${PORT}`);
});