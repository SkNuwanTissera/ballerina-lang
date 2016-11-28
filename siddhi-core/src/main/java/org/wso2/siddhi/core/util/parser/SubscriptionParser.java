/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.siddhi.core.util.parser;

import org.wso2.siddhi.core.config.ExecutionPlanContext;
import org.wso2.siddhi.core.event.stream.MetaStreamEvent;
import org.wso2.siddhi.core.event.stream.converter.ZeroStreamEventConverter;
import org.wso2.siddhi.core.exception.ExecutionPlanCreationException;
import org.wso2.siddhi.core.query.output.callback.OutputCallback;
import org.wso2.siddhi.core.query.output.ratelimit.OutputRateLimiter;
import org.wso2.siddhi.core.query.output.ratelimit.snapshot.WrappedSnapshotOutputRateLimiter;
import org.wso2.siddhi.core.subscription.InputMapper;
import org.wso2.siddhi.core.subscription.InputTransport;
import org.wso2.siddhi.core.subscription.SubscriptionRuntime;
import org.wso2.siddhi.core.table.EventTable;
import org.wso2.siddhi.core.util.SiddhiClassLoader;
import org.wso2.siddhi.core.util.SiddhiConstants;
import org.wso2.siddhi.core.util.extension.holder.InputMapperExecutorExtensionHolder;
import org.wso2.siddhi.core.util.extension.holder.InputTransportExecutorExtensionHolder;
import org.wso2.siddhi.core.util.lock.LockSynchronizer;
import org.wso2.siddhi.core.util.lock.LockWrapper;
import org.wso2.siddhi.core.util.statistics.LatencyTracker;
import org.wso2.siddhi.core.window.EventWindow;
import org.wso2.siddhi.query.api.annotation.Element;
import org.wso2.siddhi.query.api.definition.AbstractDefinition;
import org.wso2.siddhi.query.api.definition.Attribute;
import org.wso2.siddhi.query.api.definition.StreamDefinition;
import org.wso2.siddhi.query.api.exception.DuplicateDefinitionException;
import org.wso2.siddhi.query.api.execution.Subscription;
import org.wso2.siddhi.query.api.extension.Extension;
import org.wso2.siddhi.query.api.util.AnnotationHelper;

import java.util.Map;

public class SubscriptionParser {
// TODO: 11/23/16 fix this

    /**
     * Parse a subscription and return corresponding QueryRuntime.
     *
     * @param subscription         subscription to be parsed.
     * @param executionPlanContext associated Execution Plan context.
     * @param streamDefinitionMap  map containing user given stream definitions.
     * @param tableDefinitionMap   map containing table definitions.
     * @param eventTableMap        map containing event tables.
     * @return SubscriptionRuntime.
     */
    public static SubscriptionRuntime parse(final Subscription subscription, ExecutionPlanContext executionPlanContext,
                                            Map<String, AbstractDefinition> streamDefinitionMap,
                                            Map<String, AbstractDefinition> tableDefinitionMap,
                                            Map<String, AbstractDefinition> windowDefinitionMap,
                                            Map<String, EventTable> eventTableMap,
                                            Map<String, EventWindow> eventWindowMap,
                                            LockSynchronizer lockSynchronizer) {
        SubscriptionRuntime subscriptionRuntime;
        Element nameElement = null;
        LatencyTracker latencyTracker = null;
        LockWrapper lockWrapper = null;
        try {
            nameElement = AnnotationHelper.getAnnotationElement("info", "name", subscription.getAnnotations());
            if (executionPlanContext.isStatsEnabled() && executionPlanContext.getStatisticsManager() != null) {
                if (nameElement != null) {
                    String metricName =
                            executionPlanContext.getSiddhiContext().getStatisticsConfiguration().getMatricPrefix() +
                                    SiddhiConstants.METRIC_DELIMITER + SiddhiConstants.METRIC_INFIX_EXECUTION_PLANS +
                                    SiddhiConstants.METRIC_DELIMITER + executionPlanContext.getName() +
                                    SiddhiConstants.METRIC_DELIMITER + SiddhiConstants.METRIC_INFIX_SIDDHI +
                                    SiddhiConstants.METRIC_DELIMITER + SiddhiConstants.METRIC_INFIX_QUERIES +
                                    SiddhiConstants.METRIC_DELIMITER + nameElement.getValue();
                    latencyTracker = executionPlanContext.getSiddhiContext()
                            .getStatisticsConfiguration()
                            .getFactory()
                            .createLatencyTracker(metricName, executionPlanContext.getStatisticsManager());
                }
            }

            Extension transportExtension = new Extension() {
                @Override
                public String getNamespace() {
                    return SiddhiConstants.INPUT_TRANSPORT;
                }

                @Override
                public String getFunction() {
                    return subscription.getTransport().getType();
                }
            };
            InputTransport inputTransport = (InputTransport) SiddhiClassLoader.loadExtensionImplementation(transportExtension,
                    InputTransportExecutorExtensionHolder.getInstance(executionPlanContext));

            Extension mapperExtension = new Extension() {
                @Override
                public String getNamespace() {
                    return SiddhiConstants.INPUT_MAPPER;
                }

                @Override
                public String getFunction() {
                    return subscription.getMapping().getFormat();
                }
            };
            InputMapper inputMapper = (InputMapper) SiddhiClassLoader.loadExtensionImplementation(mapperExtension,
                    InputMapperExecutorExtensionHolder.getInstance(executionPlanContext));

            StreamDefinition outputStreamDefinition = (StreamDefinition) streamDefinitionMap.get(subscription.getOutputStream().getId());
            if (outputStreamDefinition == null) {
                outputStreamDefinition = (StreamDefinition) windowDefinitionMap.get(subscription.getOutputStream().getId());
            }

            inputMapper.inferOutputStreamDefinition(outputStreamDefinition);

            OutputCallback outputCallback = OutputParser.constructOutputCallback(subscription.getOutputStream(), inputMapper.getOutputStreamDefinition(),
                    eventTableMap, eventWindowMap, executionPlanContext, false);

            MetaStreamEvent metaStreamEvent = new MetaStreamEvent();
            metaStreamEvent.setOutputDefinition(inputMapper.getOutputStreamDefinition());
            for(Attribute attribute: inputMapper.getOutputStreamDefinition().getAttributeList()){
                metaStreamEvent.addOutputData(attribute);
            }
            //todo create event creator and pass to init()
            inputMapper.init(outputCallback, metaStreamEvent);

            OutputRateLimiter outputRateLimiter = OutputParser.constructOutputRateLimiter(subscription.getOutputStream().getId(),
                    subscription.getOutputRate(), false, false, executionPlanContext.getScheduledExecutorService(), executionPlanContext);
            subscriptionRuntime = new SubscriptionRuntime(inputTransport, inputMapper, outputRateLimiter, outputCallback);

            executionPlanContext.addEternalReferencedHolder(inputTransport);
            executionPlanContext.addEternalReferencedHolder(outputRateLimiter);

            if (outputRateLimiter instanceof WrappedSnapshotOutputRateLimiter) {
                throw new ExecutionPlanCreationException("Snapshot rate limiting not supported in subscription of name:" +
                        nameElement + " type:" + subscription.getTransport().getType());
            }
            outputRateLimiter.init(executionPlanContext, null);


            subscriptionRuntime.init(subscription.getTransport().getOptions(), executionPlanContext);

        } catch (DuplicateDefinitionException e) {
            if (nameElement != null) {
                throw new DuplicateDefinitionException(e.getMessage() + ", when creating subscription " + nameElement.getValue(), e);
            } else {
                throw new DuplicateDefinitionException(e.getMessage(), e);
            }
        } catch (RuntimeException e) {
            if (nameElement != null) {
                throw new ExecutionPlanCreationException(e.getMessage() + ", when creating subscription " + nameElement.getValue(), e);
            } else {
                throw new ExecutionPlanCreationException(e.getMessage(), e);
            }
        }
        return subscriptionRuntime;
    }
}
