package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.DayType;
import java.time.LocalDate;
import java.util.List;

public interface BlockScheduleService {

    Schedule generateBlockSchedule(List<Student> students, List<Course> courses,
                                   List<Teacher> teachers, List<Room> rooms);

    boolean isOddDay(LocalDate date);

    boolean isEvenDay(LocalDate date);

    DayType getDayType(LocalDate date);

    List<Course> getCoursesForDayType(Student student, DayType dayType);

    void assignCoursesToDays(Student student, List<Course> oddDayCourses, List<Course> evenDayCourses);

    List<ScheduleSlot> getSlotsForDayType(Schedule schedule, DayType dayType);
}
