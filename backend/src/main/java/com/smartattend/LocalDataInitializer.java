package com.smartattend;

import com.smartattend.domain.*;
import com.smartattend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
// Sample data is opt-in only. Normal local runs leave the database empty so
// administrators can enter and verify their own records.
@Profile("seed-local-data")
public class LocalDataInitializer {

    @Bean
    CommandLineRunner seedLocalData(
            DepartmentRepository departments,
            SectionRepository sections,
            SubjectRepository subjects,
            AppUserRepository users,
            FacultyRepository facultyRepository,
            FacultySubjectRepository facultySubjectRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {
            // 1. Seed Intermediate Streams / Departments (MPC, BiPC, MEC, CEC)
            Department mpc = departments.findByCode("MPC")
                .orElseGet(() -> departments.save(new Department("MPC", "Maths, Physics & Chemistry Stream")));
            Department bipc = departments.findByCode("BIPC")
                .orElseGet(() -> departments.save(new Department("BIPC", "Biology, Physics & Chemistry Stream")));
            Department mec = departments.findByCode("MEC")
                .orElseGet(() -> departments.save(new Department("MEC", "Maths, Economics & Commerce Stream")));
            Department cec = departments.findByCode("CEC")
                .orElseGet(() -> departments.save(new Department("CEC", "Civics, Economics & Commerce Stream")));

            // 2. Seed Intermediate Sections (MPC 1st/2nd Year, BiPC 1st/2nd Year, etc.)
            if (sections.count() == 0 || sections.findAll().stream().noneMatch(s -> s.getName().contains("MPC"))) {
                sections.save(new Section(mpc, "MPC 1st Year - Sec A", (short) 1));
                sections.save(new Section(mpc, "MPC 1st Year - Sec B", (short) 1));
                sections.save(new Section(mpc, "MPC 2nd Year - Sec A", (short) 2));
                sections.save(new Section(mpc, "MPC 2nd Year - Sec B", (short) 2));
                sections.save(new Section(bipc, "BiPC 1st Year - Sec A", (short) 1));
                sections.save(new Section(bipc, "BiPC 2nd Year - Sec A", (short) 2));
                sections.save(new Section(mec, "MEC 1st Year - Sec A", (short) 1));
                sections.save(new Section(cec, "CEC 1st Year - Sec A", (short) 1));
            }

            // 3. Seed Intermediate Subjects (MPC & BiPC Subjects)
            if (subjects.count() == 0 || subjects.findAll().stream().noneMatch(s -> s.getCode().startsWith("MATH") || s.getCode().startsWith("BOT"))) {
                // MPC & Common Subjects
                Subject math1a = getOrCreateSubject(subjects, mpc, "MATH1A", "Mathematics 1A");
                Subject math1b = getOrCreateSubject(subjects, mpc, "MATH1B", "Mathematics 1B");
                Subject math2a = getOrCreateSubject(subjects, mpc, "MATH2A", "Mathematics 2A");
                Subject math2b = getOrCreateSubject(subjects, mpc, "MATH2B", "Mathematics 2B");
                Subject phy1 = getOrCreateSubject(subjects, mpc, "PHY1", "Physics (Paper 1)");
                Subject phy2 = getOrCreateSubject(subjects, mpc, "PHY2", "Physics (Paper 2)");
                Subject che1 = getOrCreateSubject(subjects, mpc, "CHE1", "Chemistry (Paper 1)");
                Subject che2 = getOrCreateSubject(subjects, mpc, "CHE2", "Chemistry (Paper 2)");
                Subject eng1 = getOrCreateSubject(subjects, mpc, "ENG1", "English (General)");
                Subject san1 = getOrCreateSubject(subjects, mpc, "SAN1", "Sanskrit / Second Language");

                // BiPC Subjects
                Subject bot1 = getOrCreateSubject(subjects, bipc, "BOT1", "Botany (Paper 1)");
                Subject bot2 = getOrCreateSubject(subjects, bipc, "BOT2", "Botany (Paper 2)");
                Subject zoo1 = getOrCreateSubject(subjects, bipc, "ZOO1", "Zoology (Paper 1)");
                Subject zoo2 = getOrCreateSubject(subjects, bipc, "ZOO2", "Zoology (Paper 2)");

                // MEC / CEC Subjects
                Subject eco1 = getOrCreateSubject(subjects, mec, "ECO1", "Economics");
                Subject com1 = getOrCreateSubject(subjects, mec, "COM1", "Commerce & Accountancy");
                Subject civ1 = getOrCreateSubject(subjects, cec, "CIV1", "Civics");

                // 4. Seed Intermediate Faculty Lecturers
                String defaultPass = passwordEncoder.encode("pass1234");

                Faculty fMath1 = getOrCreateFaculty(users, facultyRepository, "Prof. V. K. Murthy (Maths)", "vk.murthy@smartattend.edu", defaultPass);
                Faculty fMath2 = getOrCreateFaculty(users, facultyRepository, "Dr. S. Ramanujan (Maths)", "s.ramanujan@smartattend.edu", defaultPass);
                Faculty fPhy = getOrCreateFaculty(users, facultyRepository, "Dr. C. V. Raman (Physics)", "cv.raman@smartattend.edu", defaultPass);
                Faculty fChe = getOrCreateFaculty(users, facultyRepository, "Prof. K. Sarojini (Chemistry)", "k.sarojini@smartattend.edu", defaultPass);
                Faculty fBio1 = getOrCreateFaculty(users, facultyRepository, "Dr. M. S. Swaminathan (Botany)", "ms.swaminathan@smartattend.edu", defaultPass);
                Faculty fBio2 = getOrCreateFaculty(users, facultyRepository, "Dr. Hargobind Khorana (Zoology)", "h.khorana@smartattend.edu", defaultPass);
                Faculty fEng = getOrCreateFaculty(users, facultyRepository, "Prof. R. K. Narayan (English)", "rk.narayan@smartattend.edu", defaultPass);

                // 5. Seed Faculty-Subject Mappings
                mapFacultySubject(facultySubjectRepository, fMath1, math1a);
                mapFacultySubject(facultySubjectRepository, fMath1, math1b);
                mapFacultySubject(facultySubjectRepository, fMath2, math2a);
                mapFacultySubject(facultySubjectRepository, fMath2, math2b);

                mapFacultySubject(facultySubjectRepository, fPhy, phy1);
                mapFacultySubject(facultySubjectRepository, fPhy, phy2);

                mapFacultySubject(facultySubjectRepository, fChe, che1);
                mapFacultySubject(facultySubjectRepository, fChe, che2);

                mapFacultySubject(facultySubjectRepository, fBio1, bot1);
                mapFacultySubject(facultySubjectRepository, fBio1, bot2);
                mapFacultySubject(facultySubjectRepository, fBio2, zoo1);
                mapFacultySubject(facultySubjectRepository, fBio2, zoo2);

                mapFacultySubject(facultySubjectRepository, fEng, eng1);
                mapFacultySubject(facultySubjectRepository, fEng, san1);
            }
        };
    }

    private Subject getOrCreateSubject(SubjectRepository repo, Department dept, String code, String name) {
        return repo.findAll().stream().filter(s -> s.getCode().equalsIgnoreCase(code)).findFirst()
            .orElseGet(() -> repo.save(new Subject(dept, code, name)));
    }

    private Faculty getOrCreateFaculty(AppUserRepository users, FacultyRepository facultyRepository, String name, String email, String encodedPassword) {
        return users.findByEmail(email).map(u -> facultyRepository.findByUserId(u.getId()).orElseGet(() -> facultyRepository.save(new Faculty(u))))
            .orElseGet(() -> {
                AppUser user = users.save(new AppUser(name, email, encodedPassword, Role.FACULTY));
                return facultyRepository.save(new Faculty(user));
            });
    }

    private void mapFacultySubject(FacultySubjectRepository repo, Faculty faculty, Subject subject) {
        if (!repo.existsByFacultyIdAndSubjectId(faculty.getId(), subject.getId())) {
            repo.save(new FacultySubject(faculty, subject));
        }
    }
}
