package com.aicourse.repo;

import com.aicourse.model.Module;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ModuleRepo extends JpaRepository<Module, Long> {
    List<Module> findByCourse_Title(String course);

    List<Module> findByCourse_Id(Long courseId);
}