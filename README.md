# 🏫 Google Classroom Clone

A full-stack web-based classroom management system built to replicate core functionalities of Google Classroom. Designed for teachers, teaching assistants, and students, it streamlines assignment workflows, course content delivery, grading, and team collaboration using an MVC architecture.

---

## 🧩 Tech Stack

- **Frontend**: HTML, CSS, JavaScript
- **Backend**: Java, Spring Boot
- **Database**: MySQL
- **Authentication**: Custom Login System (role-based)
- **Architecture**: Model–View–Controller (MVC)

---

## 🎯 Features

- 👩‍🏫 **Role-Based Access**: Teachers, TAs, and Students with distinct permissions  
- 📚 **Course Management**: Course creation, enrollment via invite codes  
- 📄 **Content Distribution**: Upload files, links, and announcements per course  
- 📥 **Assignments**: Create, submit, and grade with deadline support  
- 🧑‍🤝‍🧑 **Team Collaboration**: Form groups for collaborative submissions  
- 📬 **Notifications**: Real-time update alerts for new posts or grades  
- 📊 **Dashboard Views**: Custom UI based on logged-in user role  
- 📁 **Document Handling**: Upload, download, and manage materials  
- ✅ **Grading System**: Inline evaluation and structured gradebook

---

## 🏗️ System Design

### 📐 Architecture Pattern: MVC
- **Model**: Entity classes like `User`, `Course`, `Assignment`
- **View**: HTML templates + CSS + JS for dynamic rendering
- **Controller**: REST endpoints managing user and course actions

### ⚙️ Design Principles Followed:
- **Single Responsibility Principle** – Separate services for each module  
- **Open/Closed Principle** – Extendable enum types for content formats  
- **Interface Segregation & Dependency Inversion** – Clean abstraction layers  

### 🧱 Design Patterns:
- **Builder Pattern** – For entity creation  
- **Observer Pattern** – For course update notifications  

---

