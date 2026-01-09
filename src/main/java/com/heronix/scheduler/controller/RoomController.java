package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Room REST API Controller - FIXED
 * Location: src/main/java/com/eduscheduler/controller/RoomController.java
 */
@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @GetMapping
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable Long id) {
        return roomRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * ✅ FIXED: Removed null check on primitive boolean
     */
    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        // Always set active for new rooms
        room.setActive(true);

        Room saved = roomRepository.save(room);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long id,
            @RequestBody Room roomDetails) {

        return roomRepository.findById(id)
                .map(room -> {
                    room.setRoomNumber(roomDetails.getRoomNumber());
                    room.setBuilding(roomDetails.getBuilding());
                    room.setFloor(roomDetails.getFloor());
                    room.setCapacity(roomDetails.getCapacity());
                    room.setType(roomDetails.getType());
                    room.setEquipment(roomDetails.getEquipment());
                    room.setHasProjector(roomDetails.getHasProjector());
                    room.setHasSmartboard(roomDetails.getHasSmartboard());
                    room.setHasComputers(roomDetails.getHasComputers());
                    room.setWheelchairAccessible(roomDetails.getWheelchairAccessible());
                    room.setActive(roomDetails.getActive());
                    room.setNotes(roomDetails.getNotes());

                    Room updated = roomRepository.save(room);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable Long id) {
        return roomRepository.findById(id)
                .map(room -> {
                    room.setActive(false);
                    roomRepository.save(room);
                    return ResponseEntity.ok().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/active")
    public List<Room> getActiveRooms() {
        return roomRepository.findByActiveTrue();
    }

    @GetMapping("/number/{roomNumber}")
    public ResponseEntity<Room> getRoomByNumber(@PathVariable String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/building/{building}")
    public List<Room> getRoomsByBuilding(@PathVariable String building) {
        return roomRepository.findByBuilding(building);
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Room>> getRoomsByType(@PathVariable String type) {
        try {
            RoomType roomType = RoomType.valueOf(type.toUpperCase());
            List<Room> rooms = roomRepository.findByType(roomType);
            return ResponseEntity.ok(rooms);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/capacity/{minCapacity}")
    public List<Room> getRoomsByMinCapacity(@PathVariable int minCapacity) {
        return roomRepository.findByMinimumCapacity(minCapacity);
    }

    @GetMapping("/accessible")
    public List<Room> getAccessibleRooms() {
        return roomRepository.findByWheelchairAccessibleTrueAndActiveTrue();
    }

    @GetMapping("/with-projector")
    public List<Room> getRoomsWithProjector() {
        return roomRepository.findByHasProjectorTrueAndActiveTrue();
    }

    @GetMapping("/with-computers")
    public List<Room> getRoomsWithComputers() {
        return roomRepository.findByHasComputersTrueAndActiveTrue();
    }

    @GetMapping("/equipment/{equipment}")
    public List<Room> getRoomsByEquipment(@PathVariable String equipment) {
        return roomRepository.findByEquipment(equipment);
    }

    @PatchMapping("/{id}/utilization")
    public ResponseEntity<Room> updateUtilization(
            @PathVariable Long id,
            @RequestParam double rate) {

        return roomRepository.findById(id)
                .map(room -> {
                    room.setUtilizationRate(rate);
                    Room updated = roomRepository.save(room);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}