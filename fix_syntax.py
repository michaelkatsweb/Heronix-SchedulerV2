import os

# Fix SmartCourseAssignmentService
file_path = r"H:\Heronix\Heronix-SchedulerV2\src\main\java\com\heronix\scheduler\service\impl\SmartCourseAssignmentService.java"
with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Fix line 253-254
content = content.replace(
    """        // ENHANCED: Use new SubjectCertification entity with grade-level validation
        boolean isCertifiedForCourse = // TODO: Method does not exist - implement certification check
                false && teacher.getName() != null && // teacher.hasCertificationForSubjectAndGrade(course.getSubject(), gradeLevel);""",
    """        // ENHANCED: Use new SubjectCertification entity with grade-level validation
        // TODO: Method hasCertificationForSubjectAndGrade does not exist - implement certification check
        boolean isCertifiedForCourse = false; // teacher.hasCertificationForSubjectAndGrade(course.getSubject(), gradeLevel);"""
)

# Fix line 318-319
content = content.replace(
    """        if (isCertifiedForCourse && // TODO: Method does not exist - implement expiration check
                false // teacher.hasExpiringCertifications()) {""",
    """        // TODO: Method hasExpiringCertifications does not exist - implement expiration check
        if (isCertifiedForCourse && false) { // teacher.hasExpiringCertifications()) {"""
)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed SmartCourseAssignmentService.java")

# Fix RoomsController
file_path2 = r"H:\Heronix\Heronix-SchedulerV2\src\main\java\com\heronix\scheduler\controller\ui\RoomsController.java"
with open(file_path2, 'r', encoding='utf-8') as f:
    content2 = f.read()

# Find and fix the TODO comment issue at line 758
content2 = content2.replace(
    """                    // TODO: Method generateRoomPhoneNumber() does not exist
                    // districtSettingsService.generateRoomPhoneNumber(""",
    """                    // TODO: Method generateRoomPhoneNumber() does not exist
                    String phoneNumber = null; // districtSettingsService.generateRoomPhoneNumber("""
)

with open(file_path2, 'w', encoding='utf-8') as f:
    f.write(content2)
print("Fixed RoomsController.java")
