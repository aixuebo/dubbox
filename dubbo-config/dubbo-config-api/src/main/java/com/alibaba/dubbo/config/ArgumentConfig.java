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
package com.alibaba.dubbo.config;

import java.io.Serializable;

import com.alibaba.dubbo.config.support.Parameter;

/**
 * @author chao.liuc
 * @export
<dubbo:service id="registryServiceConfig" interface="com.alibaba.dubbo.registry.RegistryService" ref="registryService" registry="N/A" ondisconnect="disconnect" callbacks="1000">
    <dubbo:method name="subscribe"><dubbo:argument index="1" callback="true" /></dubbo:method>
    <dubbo:method name="unsubscribe"><dubbo:argument index="1" callback="false" /></dubbo:method>
</dubbo:service>
 * 表示第几个参数,以及参数的类型
 */
public class ArgumentConfig implements Serializable {

    private static final long serialVersionUID = -2165482463925213595L;

    //arugment index -1 represents not set
    private Integer index = -1;

    //argument type
    private String  type;
    
    //callback interface
    private Boolean callback;

    public void setIndex(Integer index) {
        this.index = index;
    }
    @Parameter(excluded = true)
    public Integer getIndex() {
        return index;
    }
    @Parameter(excluded = true)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCallback(Boolean callback) {
        this.callback = callback;
    }

    public Boolean isCallback() {
        return callback;
    }

}