package com.aicourse.model;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CourseTest {

        @Test
        public void testCourseConstructorAndGetters() {
            Course course = new Course();
            course.setId(1L);
            course.setTitle("Test Course");
            course.setDescription("This is a test course.");

            course.setProjectId(1343554L);
            assertEquals(1343554L, course.getProjectId());

            course.setCreator(123456789L);
            assertEquals(123456789L, course.getCreator());

            List<Module> modules = new ArrayList<>();
            Module module1 = new Module();
            module1.setId(1L);
            module1.setTitle("Module 1");

            Module module2 = new Module();
            module2.setId(2L);
            module2.setTitle("Module 2");

            modules.add(module1);
            modules.add(module2);

            course.setModules(modules);
            module2.setCourse(course);
            module1.setCourse(course);
            assertEquals(course, module2.getCourse());
            assertEquals("Module 2", module2.getTitle());
            List<Lesson> lessons = new ArrayList<>();
            Lesson lesson1 = new Lesson();
            lesson1.setId(1L);
            lesson1.setTitle("Lesson 1");

            Lesson lesson2 = new Lesson();
            lesson2.setId(2L);
            lesson2.setTitle("Lesson 2");
            lessons.add(lesson1);
            lessons.add(lesson2);
            module2.setLessons( lessons);

            assertEquals(modules, course.getModules());
            assertTrue(course.isNew());

            assertTrue(module2.isNew());

            assertEquals(1L, course.getId());
            assertEquals("Test Course", course.getTitle());
            assertEquals("This is a test course.", course.getDescription());

        }


}