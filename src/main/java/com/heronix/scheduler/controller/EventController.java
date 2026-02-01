package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.Event;
import com.heronix.scheduler.model.enums.EventType;
import com.heronix.scheduler.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event REST API Controller - FIXED
 * Location: src/main/java/com/eduscheduler/controller/EventController.java
 * 
 * Endpoints:
 * - GET /api/events - Get all events
 * - GET /api/events/{id} - Get event by ID
 * - POST /api/events - Create new event
 * - PUT /api/events/{id} - Update event
 * - DELETE /api/events/{id} - Delete event
 * - GET /api/events/type/{type} - Get events by type
 * - GET /api/events/upcoming - Get upcoming events
 * - GET /api/events/blocking - Get blocking events
 * - GET /api/events/compliance - Get compliance meetings
 * - GET /api/events/date-range - Get events in date range
 */
@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = {"http://localhost:9580", "http://localhost:9585", "http://localhost:9590", "http://localhost:58280", "http://localhost:58180"})
public class EventController {

    @Autowired
    private EventRepository eventRepository;

    /**
     * GET /api/events
     * Get all events
     */
    @GetMapping
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    /**
     * GET /api/events/{id}
     * Get event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Event> getEventById(@PathVariable Long id) {
        return eventRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/events
     * Create new event
     */
    @PostMapping
    public ResponseEntity<Event> createEvent(@RequestBody Event event) {
        // Set timestamps
        if (event.getStartDateTime() == null) {
            event.setStartDateTime(LocalDateTime.now());
        }
        if (event.getEndDateTime() == null) {
            event.setEndDateTime(event.getStartDateTime().plusHours(1));
        }

        Event saved = eventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * PUT /api/events/{id}
     * Update existing event
     */
    @PutMapping("/{id}")
    public ResponseEntity<Event> updateEvent(
            @PathVariable Long id,
            @RequestBody Event eventDetails) {

        return eventRepository.findById(id)
                .map(event -> {
                    event.setName(eventDetails.getName());
                    event.setEventType(eventDetails.getEventType());
                    event.setStartDateTime(eventDetails.getStartDateTime());
                    event.setEndDateTime(eventDetails.getEndDateTime());
                    event.setDescription(eventDetails.getDescription());
                    event.setBlocksScheduling(eventDetails.getBlocksScheduling());
                    event.setAllDay(eventDetails.getAllDay());
                    event.setRecurring(eventDetails.getRecurring());
                    event.setRecurrencePattern(eventDetails.getRecurrencePattern());
                    event.setNotes(eventDetails.getNotes());
                    event.setAffectedTeachers(eventDetails.getAffectedTeachers());
                    event.setAffectedRooms(eventDetails.getAffectedRooms());

                    Event updated = eventRepository.save(event);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/events/{id}
     * Delete event
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {
        return eventRepository.findById(id)
                .map(event -> {
                    eventRepository.delete(event);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/events/type/{type}
     * Get events by type
     * ✅ FIXED: Changed from findByType to findByEventType
     */
    @GetMapping("/type/{type}")
    public List<Event> getEventsByType(@PathVariable EventType type) {
        return eventRepository.findByEventType(type);
    }

    /**
     * GET /api/events/upcoming
     * Get upcoming events
     */
    @GetMapping("/upcoming")
    public List<Event> getUpcomingEvents() {
        return eventRepository.findUpcomingEvents(LocalDateTime.now());
    }

    /**
     * GET /api/events/blocking
     * Get all blocking events
     * ✅ FIXED: Changed from findByBlockSchedulingTrue to findByBlocksSchedulingTrue
     */
    @GetMapping("/blocking")
    public List<Event> getBlockingEvents() {
        return eventRepository.findByBlocksSchedulingTrue();
    }

    /**
     * GET /api/events/recurring
     * Get all recurring events
     */
    @GetMapping("/recurring")
    public List<Event> getRecurringEvents() {
        return eventRepository.findByRecurringTrue();
    }

    /**
     * GET /api/events/all-day
     * Get all-day events
     */
    @GetMapping("/all-day")
    public List<Event> getAllDayEvents() {
        return eventRepository.findByAllDayTrue();
    }

    /**
     * GET /api/events/compliance
     * Get compliance meetings (IEP, 504, etc.)
     */
    @GetMapping("/compliance")
    public List<Event> getComplianceMeetings() {
        return eventRepository.findComplianceMeetings();
    }

    /**
     * GET /api/events/date-range
     * Get events within date range
     */
    @GetMapping("/date-range")
    public List<Event> getEventsInDateRange(
            @RequestParam LocalDateTime startDateTime,
            @RequestParam LocalDateTime endDateTime) {
        return eventRepository.findEventsInDateRange(startDateTime, endDateTime);
    }

    /**
     * GET /api/events/teacher/{teacherId}
     * Get events affecting specific teacher
     */
    @GetMapping("/teacher/{teacherId}")
    public List<Event> getEventsByTeacher(@PathVariable Long teacherId) {
        return eventRepository.findByAffectedTeacherId(teacherId);
    }

    /**
     * GET /api/events/room/{roomId}
     * Get events affecting specific room
     */
    @GetMapping("/room/{roomId}")
    public List<Event> getEventsByRoom(@PathVariable Long roomId) {
        return eventRepository.findByAffectedRoomId(roomId);
    }
}