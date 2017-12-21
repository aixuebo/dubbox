package com.alibaba.dubbo.container.javaconfig.demo.resource;


import com.alibaba.dubbo.container.javaconfig.demo.Assignment;
import com.alibaba.dubbo.container.javaconfig.demo.Course;
import com.alibaba.dubbo.container.javaconfig.demo.Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppContext {
    @Bean
    public Course course() {
        Course course = new Course();
        course.setModule(module());
        return course;
    }

    @Bean
    public Module module() {
        Module module = new Module();
        module.setAssignment(assignment());
        return module;
    }

    @Bean
    public Assignment assignment() {
        return new Assignment();
    }
}