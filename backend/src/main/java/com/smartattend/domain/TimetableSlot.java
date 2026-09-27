package com.smartattend.domain;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "timetable_slots")
public class TimetableSlot {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "section_id") private Section section;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "subject_id") private Subject subject;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "faculty_id") private Faculty faculty;
    @Column(name = "day_of_week", nullable = false) private byte dayOfWeek;
    @Column(name = "start_time", nullable = false) private LocalTime startTime;
    @Column(name = "end_time", nullable = false) private LocalTime endTime;
    @Column(nullable = false, length = 80) private String room;
    protected TimetableSlot() { }
    public TimetableSlot(Section section, Subject subject, Faculty faculty, byte dayOfWeek, LocalTime startTime, LocalTime endTime, String room) { this.section = section; this.subject = subject; this.faculty = faculty; this.dayOfWeek = dayOfWeek; this.startTime = startTime; this.endTime = endTime; this.room = room; }
    public Long getId() { return id; }
    public Section getSection() { return section; }
    public Subject getSubject() { return subject; }
    public Faculty getFaculty() { return faculty; }
    public byte getDayOfWeek() { return dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getRoom() { return room; }
}
