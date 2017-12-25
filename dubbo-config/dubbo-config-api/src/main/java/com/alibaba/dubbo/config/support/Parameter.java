/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.config.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameter
 * 参数对象
 * 用于在属性的get方法上面,可以对get的name进行修改,修改成key,作为参数进行接下来的传递
 *
 * 比如getName上有这个Parameter,key为xxx,因此参数传递的就不是name了,而是xxx
 * @author william.liangf
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Parameter {

    String key() default "";

    //必须存在
    boolean required() default false;
    
    boolean excluded() default false;

    //true表示要对该key对应的属性值进行转码处理,即URL.encode(value);
    boolean escaped() default false;

    boolean attribute() default false;

    //true表示如果key对应多次值,则要进行追加填写,所有的值分割使用逗号
    boolean append() default false;
    
}