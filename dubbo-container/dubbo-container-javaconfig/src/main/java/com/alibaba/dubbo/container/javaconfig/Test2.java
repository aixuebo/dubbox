package com.alibaba.dubbo.container.javaconfig;

import com.alibaba.dubbo.container.javaconfig.demo.Course;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * 外部使用javaconf去调用程序,因为主程序在sleep
 */
public class Test2 {

    public void test1(){
        AnnotationConfigApplicationContext context = Test.context;
        Course course = ((Course) context.getBean("course"));
        course.getModule().getAssignment().test();
    }

}
