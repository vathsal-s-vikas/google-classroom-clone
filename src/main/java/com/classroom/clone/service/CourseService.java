package com.classroom.clone.service;

import com.classroom.clone.model.Course;
import com.classroom.clone.model.CourseMembership;
import com.classroom.clone.model.User;
import com.classroom.clone.repository.CourseMembershipRepository;
import com.classroom.clone.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseMembershipRepository membershipRepository;
    private final UserService userService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int INVITE_CODE_LENGTH = 6;
    private static final int COURSE_CODE_LENGTH = 8;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public CourseService(CourseRepository courseRepository,
                         CourseMembershipRepository membershipRepository,
                         UserService userService) {
        this.courseRepository = courseRepository;
        this.membershipRepository = membershipRepository;
        this.userService = userService;
    }

    /**
     * Creates a new course and adds the teacher as a member with TEACHER role
     * @param course The course to create
     * @param teacher The teacher who creates the course
     * @return The created course
     */
    public Course createCourse(Course course, User teacher) {
        // Generate course code and invite code
        course.setCourseCode(generateCourseCode());
        course.setInviteCode(generateInviteCode());
        course.setTeacher(teacher);
        course.setArchived(false);

        Course savedCourse = courseRepository.save(course);

        // Create membership for the teacher
        CourseMembership teacherMembership = new CourseMembership();
        teacherMembership.setCourse(savedCourse);
        teacherMembership.setUser(teacher);
        teacherMembership.setRole(CourseMembership.Role.valueOf("TEACHER"));
        membershipRepository.save(teacherMembership);

        return savedCourse;
    }



    /**
     * Adds a user to a course with the specified role
     *
     * @param course The course
     * @param user   The user to add
     * @param role   The role (STUDENT or TA)
     */
    public void addUserToCourse(Course course, User user, String role) {
        // Check if user is already in the course
        Optional<CourseMembership> existingMembership = membershipRepository.findByCourseAndUser(course, user);
        if (existingMembership.isPresent()) {
            // Update role if different
            CourseMembership membership = existingMembership.get();
            membership.setRole(CourseMembership.Role.valueOf(role));
            membershipRepository.save(membership);
            return;
        }

        // Create new membership
        CourseMembership membership = new CourseMembership();
        membership.setCourse(course);
        membership.setUser(user);
        membership.setRole(CourseMembership.Role.valueOf(role));
        membershipRepository.save(membership);
    }

    /**
     * Enrolls a student in a course using an invite code
     * @param inviteCode The invite code
     * @param student The student
     * @return The course if enrollment was successful, empty if invite code is invalid
     */
    public Optional<Course> enrollStudentWithInviteCode(String inviteCode, User student) {
        Optional<Course> courseOptional = courseRepository.findByInviteCode(inviteCode);
        if (courseOptional.isPresent()) {
            Course course = courseOptional.get();
            // Don't enroll if course is archived
            if (course.isArchived()) {
                return Optional.empty();
            }

            addUserToCourse(course, student, "STUDENT");
            return Optional.of(course);
        }
        return Optional.empty();
    }

    /**
     * Removes a user from a course
     * @param course The course
     * @param user The user to remove
     */
    public void removeUserFromCourse(Course course, User user) {
        membershipRepository.deleteByCourseAndUser(course, user);
    }

    /**
     * Archives or unarchives a course
     * @param courseId The course ID
     * @param archive Whether to archive (true) or unarchive (false)
     * @return The updated course if found, empty otherwise
     */
    public Optional<Course> setArchiveStatus(Integer courseId, boolean archive) {
        Optional<Course> courseOptional = courseRepository.findById(courseId);
        if (courseOptional.isPresent()) {
            Course course = courseOptional.get();
            course.setArchived(archive);
            return Optional.of(courseRepository.save(course));
        }
        return Optional.empty();
    }

    /**
     * Generates a new unique invite code
     * @return A 6-character alphanumeric invite code
     */
    private String generateInviteCode() {
        String inviteCode;
        do {
            inviteCode = generateRandomCode(INVITE_CODE_LENGTH);
        } while (courseRepository.findByInviteCode(inviteCode).isPresent());
        return inviteCode;
    }

    /**
     * Generates a new unique course code
     * @return An 8-character alphanumeric course code
     */
    private String generateCourseCode() {
        String courseCode;
        do {
            courseCode = generateRandomCode(COURSE_CODE_LENGTH);
        } while (courseRepository.findByCourseCode(courseCode).isPresent());
        return courseCode;
    }

    /**
     * Generates a random alphanumeric code of the specified length
     * @param length The length of the code
     * @return A random alphanumeric string
     */
    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    /**
     * Gets all courses taught by a teacher
     * @param teacher The teacher
     * @param includeArchived Whether to include archived courses
     * @return List of courses
     */
    public List<Course> getTeacherCourses(User teacher, boolean includeArchived) {
        if (includeArchived) {
            return courseRepository.findByTeacher(teacher);
        } else {
            return courseRepository.findByTeacherAndIsArchived(teacher, false);
        }
    }

    /**
     * Gets all courses where the user is a student
     * @param user The user
     * @return List of courses
     */
    public List<Course> getStudentCourses(User user) {
        return courseRepository.findCoursesWhereUserIsStudent(user);
    }

    /**
     * Gets all courses where the user is a teaching assistant
     * @param user The user
     * @return List of courses
     */
    public List<Course> getTACourses(User user) {
        return courseRepository.findCoursesWhereUserIsTA(user);
    }

    /**
     * Gets all courses for a user regardless of role
     * @param user The user
     * @return List of courses
     */
    public List<Course> getAllUserCourses(User user) {
        return courseRepository.findAllCoursesByUser(user);
    }

    /**
     * Refreshes an invite code for a course
     * @param courseId The course ID
     * @return The updated course if found, empty otherwise
     */
    public Optional<Course> refreshInviteCode(Integer courseId) {
        Optional<Course> courseOptional = courseRepository.findById(courseId);
        if (courseOptional.isPresent()) {
            Course course = courseOptional.get();
            course.setInviteCode(generateInviteCode());
            return Optional.of(courseRepository.save(course));
        }
        return Optional.empty();
    }

    /**
     * Gets all students in a course
     * @param courseId The course ID
     * @return List of students
     */
    public List<User> getStudentsInCourse(Integer courseId) {
        Optional<Course> courseOptional = courseRepository.findById(courseId);
        if (courseOptional.isPresent()) {
            Course course = courseOptional.get();
            return membershipRepository.findByCourseAndRole(course, "STUDENT")
                    .stream()
                    .map(CourseMembership::getUser)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    /**
     * Gets all TAs in a course
     * @param courseId The course ID
     * @return List of teaching assistants
     */
    public List<User> getTAsInCourse(Integer courseId) {
        Optional<Course> courseOptional = courseRepository.findById(courseId);
        if (courseOptional.isPresent()) {
            Course course = courseOptional.get();
            return membershipRepository.findByCourseAndRole(course, "TA")
                    .stream()
                    .map(CourseMembership::getUser)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    public boolean isUserTA(User ta, Course course) {
        // Check if the user is a TA in the course
        return membershipRepository.findByCourseAndUser(course, ta)
                .map(membership -> membership.getRole() == CourseMembership.Role.TA)
                .orElse(false);
    }
}