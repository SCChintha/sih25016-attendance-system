package com.smartattend;

import com.smartattend.domain.Department;
import com.smartattend.domain.Section;
import com.smartattend.repository.DepartmentRepository;
import com.smartattend.repository.SectionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("default")
public class LocalDataInitializer {
    @Bean
    CommandLineRunner seedLocalSection(DepartmentRepository departments, SectionRepository sections) {
        return args -> {
            if (sections.count() == 0) {
                Department department = departments.findByCode("GEN")
                    .orElseGet(() -> departments.save(new Department("GEN", "General Studies")));
                sections.save(new Section(department, "General", (short) 1));
            }
        };
    }
}