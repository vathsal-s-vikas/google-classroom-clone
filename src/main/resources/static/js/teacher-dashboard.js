let courseToDelete = null;

function toggleDropdown() {
  console.log("Toggle dropdown clicked");
  const dropdown = document.getElementById("avatarDropdown");
  dropdown.classList.toggle('show');
  console.log("Dropdown visibility:", dropdown.classList.contains('show') ? "visible" : "hidden");
}

async function fetchTeacherName() {
  try {
    const res = await fetch('/api/teacher/me');
    const data = await res.json();
    // Only update welcome-text if it exists
    const welcomeText = document.getElementById("welcome-text");
    if (welcomeText) {
      welcomeText.innerHTML = `Welcome, <strong>${data.name}</strong>`;
    }
    // Only update avatar if it exists and has text content
    const avatar = document.querySelector(".avatar");
    if (avatar && data.name) {
      avatar.textContent = data.name[0].toUpperCase();
    }
  } catch (error) {
    console.error("Error fetching teacher name:", error);
    // Only update if element exists
    const welcomeText = document.getElementById("welcome-text");
    if (welcomeText) {
      welcomeText.innerHTML = `Welcome, <strong>Teacher</strong>`;
    }
  }
}

function showToast(message, type = "success") {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  
  // Set toast color based on type
  if (type === "error") {
    toast.style.backgroundColor = "#dc3545";
  } else {
    toast.style.backgroundColor = "#4CAF50";
  }
  
  toast.classList.add("show");
  setTimeout(() => {
    toast.classList.remove("show");
  }, 3000);
}

async function createCourse(name, description) {
  try {
    console.log('Attempting to create course:', { name, description });
    
    // Get CSRF token
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || 
                      getCookie('XSRF-TOKEN');
                      
    const res = await fetch('/api/teacher/create-course', {
      method: 'POST',
      headers: { 
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      },
      body: JSON.stringify({ name, description })
    });
    
    if (res.ok) {
      console.log('Course created successfully');
      showToast("Course created successfully!");
      // Reload the page instead of trying to update the DOM
      setTimeout(() => {
        window.location.reload();
      }, 1500);
      
      // Clear form fields
      document.getElementById("className").value = '';
      document.getElementById("classDescription").value = '';
    } else {
      const errorData = await res.json().catch(() => null);
      console.error('Failed to create course. Status:', res.status, 'Error:', errorData);
      alert('Failed to create course: ' + (errorData ? JSON.stringify(errorData) : res.statusText));
    }
  } catch (error) {
    console.error('Error creating course:', error);
    alert('Error creating course: ' + error.message);
  }
}

async function deleteCourse(courseId) {
  try {
    console.log('Attempting to delete course:', courseId);
    
    // Get CSRF token from meta tag or cookie
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || 
                     getCookie('XSRF-TOKEN');
    
    console.log(`Sending DELETE request to: /api/teacher/delete-course/${courseId}`);
    console.log('Using CSRF token:', csrfToken ? 'Present' : 'Not found');
    
    const res = await fetch(`/api/teacher/delete-course/${courseId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      },
      credentials: 'same-origin' // Include cookies in the request
    });
    
    console.log('Delete response status:', res.status);
    
    if (res.ok) {
      console.log('Course deleted successfully');
      showToast("Course deleted successfully");
      // Reload the page instead of trying to update the DOM
      setTimeout(() => {
        window.location.reload();
      }, 1500);
    } else {
      let errorMessage = '';
      try {
        // Try to parse as JSON
        const errorData = await res.json();
        console.error('Failed to delete course. Status:', res.status, 'Error:', errorData);
        errorMessage = errorData ? JSON.stringify(errorData) : res.statusText;
      } catch (parseError) {
        // If not JSON, try to get text
        const errorText = await res.text();
        console.error('Failed to delete course. Status:', res.status, 'Error text:', errorText);
        errorMessage = errorText || res.statusText;
      }
      alert('Failed to delete course: ' + errorMessage);
    }
  } catch (error) {
    console.error('Error deleting course:', error);
    alert('Error deleting course: ' + error.message);
  }
}

// Helper function to get cookie value by name
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

function copyInviteCode(code, event) {
  if (!event) event = window.event; // Fallback for older browsers
  if (event) event.stopPropagation();
  
  // Get the button element that was clicked
  const button = event ? event.currentTarget : null;
  const originalIcon = button ? button.querySelector('i').className : null;
  
  // Temporarily change the icon to show copying in progress
  if (button && button.querySelector('i')) {
    button.querySelector('i').className = 'fas fa-spinner fa-spin';
  }
  
  navigator.clipboard.writeText(code)
    .then(() => {
      // Show success icon briefly
      if (button && button.querySelector('i')) {
        button.querySelector('i').className = 'fas fa-check';
      }
      
      // Show toast notification
      showToast(`Invite code copied: ${code}`);
      
      // Reset the icon after a short delay
      if (button && originalIcon) {
        setTimeout(() => {
          if (button.querySelector('i')) {
            button.querySelector('i').className = originalIcon;
          }
        }, 1000);
      }
    })
    .catch(err => {
      console.error('Error copying text: ', err);
      if (button && button.querySelector('i')) {
        button.querySelector('i').className = 'fas fa-times';
      }
      showToast("Failed to copy code", "error");
      
      // Reset the icon after a short delay
      if (button && originalIcon) {
        setTimeout(() => {
          if (button.querySelector('i')) {
            button.querySelector('i').className = originalIcon;
          }
        }, 1000);
      }
    });
}

async function loadAssignments() {
    try {
        const res = await fetch('/api/teacher/pending-assignments');
        if (!res.ok) {
            throw new Error('Failed to fetch assignments');
        }
        
        const assignments = await res.json();
        const container = document.getElementById('pending-assignments');
        
        if (!assignments || assignments.length === 0) {
            container.innerHTML = `
                <div class="no-assignments">
                    <i class="fas fa-tasks" style="font-size: 3rem; color: #ddd; margin-bottom: 1rem; display: block;"></i>
                    <h3>No assignments yet</h3>
                    <p>Create assignments in your courses to see them here.</p>
                </div>
            `;
            return;
        }
        
        // Sort assignments by deadline (closest first)
        assignments.sort((a, b) => new Date(a.deadline) - new Date(b.deadline));
        
        container.innerHTML = '';
        
        assignments.forEach(assignment => {
            const deadline = new Date(assignment.deadline);
            const isPastDeadline = deadline < new Date();
            
            const card = document.createElement('div');
            card.className = 'assignment-card';
            
            card.innerHTML = `
                <div class="assignment-header">
                    <div>
                        <h3 class="assignment-title">${assignment.title}</h3>
                        <div class="assignment-course">Course: ${assignment.courseName}</div>
                    </div>
                    <div class="assignment-type">
                        <span class="badge">${assignment.assignmentType}</span>
                    </div>
                </div>
                
                <div class="assignment-details">
                    <div class="assignment-detail">
                        <i class="fas fa-calendar"></i>
                        <span>Due: ${deadline.toLocaleDateString()} ${deadline.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                    </div>
                    <div class="assignment-detail">
                        <i class="fas fa-award"></i>
                        <span>Max marks: ${assignment.maxMarks}</span>
                    </div>
                    <div class="assignment-detail">
                        <i class="fas ${isPastDeadline ? 'fa-clock text-danger' : 'fa-hourglass-half'}"></i>
                        <span>${isPastDeadline ? 'Deadline passed' : 'Deadline approaching'}</span>
                    </div>
                </div>
                
                <div class="assignment-stats">
                    <div class="assignment-stat">
                        <span class="stat-value">${assignment.totalSubmissions}</span>
                        <span class="stat-label">Submissions</span>
                    </div>
                    <div class="assignment-stat">
                        <span class="stat-value">${assignment.pendingEvaluations}</span>
                        <span class="stat-label">Pending Evaluation</span>
                    </div>
                    <div class="assignment-actions">
                        <a href="/teacher/course/${assignment.courseId}?assignmentId=${assignment.id}" class="view-btn">
                            <i class="fas fa-eye"></i> View
                        </a>
                    </div>
                </div>
            `;
            
            container.appendChild(card);
        });
    } catch (error) {
        console.error('Error loading assignments:', error);
        const container = document.getElementById('pending-assignments');
        container.innerHTML = `
            <div class="error" style="text-align: center; padding: 2rem; color: #dc3545;">
                <i class="fas fa-exclamation-triangle" style="font-size: 2rem; margin-bottom: 1rem; display: block;"></i>
                <p>Failed to load assignments: ${error.message}</p>
            </div>
        `;
    }
}

window.onload = async () => {
  await fetchTeacherName();
  await loadAssignments();
};

document.addEventListener('click', function(e) {
  const dropdown = document.getElementById("avatarDropdown");
  if (!e.target.closest('.avatar') && dropdown.classList.contains('show')) {
    dropdown.classList.remove('show');
  }
});

// Close modals when clicking outside
window.onclick = function(event) {
  const createModal = document.getElementById("createModal");
  const deleteModal = document.getElementById("deleteModal");
  if (event.target === createModal) {
    createModal.style.display = "none";
  }
  if (event.target === deleteModal) {
    deleteModal.style.display = "none";
  }
};

// Section switching
function showSection(sectionName) {
  // Hide all sections
  document.querySelectorAll('.dashboard-section').forEach(section => {
    section.classList.remove('active');
  });
  
  // Remove active class from all sidebar menu items
  document.querySelectorAll('.sidebar-menu a').forEach(item => {
    item.classList.remove('active');
  });
  
  // Show selected section
  document.getElementById(sectionName + '-section').classList.add('active');
  
  // Add active class to selected sidebar menu item
  document.querySelector('.sidebar-menu a[onclick="showSection(\'' + sectionName + '\')"]').classList.add('active');
}

async function archiveCourse(courseId) {
  try {
    // Get CSRF token
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || 
                      getCookie('XSRF-TOKEN');
                      
    const res = await fetch(`/api/teacher/archive-course/${courseId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      }
    });
    
    if (res.ok) {
      showToast("Course archived successfully");
      // Reload the page instead of trying to update the DOM
      setTimeout(() => {
        window.location.reload();
      }, 1500);
    } else {
      const errorData = await res.json().catch(() => null);
      console.error('Failed to archive course. Status:', res.status, 'Error:', errorData);
      alert('Failed to archive course: ' + (errorData || res.statusText));
    }
  } catch (error) {
    console.error('Error archiving course:', error);
    alert('Error archiving course: ' + error.message);
  }
}

async function unarchiveCourse(courseId) {
  try {
    // Get CSRF token
    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || 
                      getCookie('XSRF-TOKEN');
                      
    const res = await fetch(`/api/teacher/unarchive-course/${courseId}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'X-CSRF-TOKEN': csrfToken
      }
    });
    
    if (res.ok) {
      showToast("Course unarchived successfully");
      // Reload the page instead of trying to update the DOM
      setTimeout(() => {
        window.location.reload();
      }, 1500);
    } else {
      const errorData = await res.json().catch(() => null);
      console.error('Failed to unarchive course. Status:', res.status, 'Error:', errorData);
      alert('Failed to unarchive course: ' + (errorData || res.statusText));
    }
  } catch (error) {
    console.error('Error unarchiving course:', error);
    alert('Error unarchiving course: ' + error.message);
  }
}

function openCreateModal() {
  document.getElementById("createModal").style.display = "block";
}

function closeModal() {
  document.getElementById("createModal").style.display = "none";
}

function openDeleteModal(courseId) {
  courseToDelete = courseId;
  document.getElementById("deleteModal").style.display = "block";
  
  // Set up confirmation button with proper error handling
  document.getElementById("confirmDeleteBtn").onclick = async function() {
    try {
      // Show loading indicator or disable button
      const btn = document.getElementById("confirmDeleteBtn");
      const originalText = btn.innerText;
      btn.innerText = "Deleting...";
      btn.disabled = true;
      
      await deleteCourse(courseId);
      
      // Reset button and close modal
      btn.innerText = originalText;
      btn.disabled = false;
      closeDeleteModal();
    } catch (error) {
      console.error("Error in delete confirmation:", error);
      alert("Failed to delete course: " + error.message);
      
      // Reset button even on error
      document.getElementById("confirmDeleteBtn").innerText = "Delete";
      document.getElementById("confirmDeleteBtn").disabled = false;
    }
  };
}

function closeDeleteModal() {
  document.getElementById("deleteModal").style.display = "none";
  courseToDelete = null;
}

async function submitCourse() {
  const name = document.getElementById("className").value.trim();
  const desc = document.getElementById("classDescription").value.trim();
  if (!name || !desc) {
    alert("Please fill all fields.");
    return;
  }
  console.log('Form submitted with values:', { name, desc });
  await createCourse(name, desc);
  closeModal();
}

function toggleCourseDropdown(button) {
  // Get the dropdown element
  const dropdown = button.nextElementSibling;
  
  // Close all other dropdowns first
  document.querySelectorAll('.course-dropdown.show').forEach(openDropdown => {
    if (openDropdown !== dropdown) {
      openDropdown.classList.remove('show');
    }
  });
  
  // Toggle the clicked dropdown
  dropdown.classList.toggle('show');
  
  // Stop propagation to prevent closing when clicking inside
  event.stopPropagation();
}

// Close dropdowns when clicking outside
document.addEventListener('click', function(e) {
  document.querySelectorAll('.course-dropdown.show').forEach(dropdown => {
    if (!dropdown.parentElement.contains(e.target)) {
      dropdown.classList.remove('show');
    }
  });
}); 