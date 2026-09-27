package com.smartattend.attendance;

import com.smartattend.domain.Section;
import com.smartattend.repository.SectionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/sections")
public class SectionController {
    private final SectionRepository sections;

    public SectionController(SectionRepository sections) {
        this.sections = sections;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<SectionResponse> list() {
        return sections.findAll().stream()
            .sorted(Comparator.comparing(Section::getName).thenComparing(Section::getSemester))
            .map(section -> new SectionResponse(section.getId(), section.getName(), section.getSemester(), section.getDepartment().getName()))
            .toList();
    }

    public record SectionResponse(Long id, String name, short semester, String department) { }
}