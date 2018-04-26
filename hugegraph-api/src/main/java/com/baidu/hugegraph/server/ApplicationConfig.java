/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.server;

import javax.ws.rs.ApplicationPath;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;

import com.baidu.hugegraph.config.HugeConfig;
import com.baidu.hugegraph.core.GraphManager;
import com.baidu.hugegraph.util.E;

@ApplicationPath("/")
public class ApplicationConfig extends ResourceConfig {

    public ApplicationConfig(HugeConfig conf) {
        packages("com.baidu.hugegraph.api");

        // Register Jackson to support json
        register(org.glassfish.jersey.jackson.JacksonFeature.class);

        // Register to use the jsr250 annotations @RolesAllowed
        register(RolesAllowedDynamicFeature.class);

        // Register HugeConfig to context
        register(new ConfFactory(conf));

        // Register GraphManager to context
        register(new GraphManagerFactory(conf));
    }

    private class ConfFactory extends AbstractBinder
                              implements Factory<HugeConfig> {

        private HugeConfig conf = null;

        public ConfFactory(HugeConfig conf) {
            E.checkNotNull(conf, "configuration");
            this.conf = conf;
        }

        @Override
        protected void configure() {
            bindFactory(this).to(HugeConfig.class).in(RequestScoped.class);
        }

        @Override
        public HugeConfig provide() {
            return this.conf;
        }

        @Override
        public void dispose(HugeConfig conf) {
            // pass
        }
    }

    private class GraphManagerFactory extends AbstractBinder
                                      implements Factory<GraphManager> {

        private GraphManager manager = null;

        public GraphManagerFactory(HugeConfig conf) {
            register(new ApplicationEventListener() {
                private final ApplicationEvent.Type EVENT_INITED =
                              ApplicationEvent.Type.INITIALIZATION_FINISHED;
                @Override
                public void onEvent(ApplicationEvent event) {
                    if (event.getType() == this.EVENT_INITED) {
                        GraphManagerFactory.this.manager = new GraphManager(conf);
                    }
                }

                @Override
                public RequestEventListener onRequest(RequestEvent event) {
                    return null;
                }
            });
        }

        @Override
        protected void configure() {
            bindFactory(this).to(GraphManager.class).in(RequestScoped.class);
        }

        @Override
        public GraphManager provide() {
            E.checkState(this.manager != null,
                         "Please wait for the server to initialize");
            return this.manager;
        }

        @Override
        public void dispose(GraphManager manager) {
            // pass
        }
    }
}
