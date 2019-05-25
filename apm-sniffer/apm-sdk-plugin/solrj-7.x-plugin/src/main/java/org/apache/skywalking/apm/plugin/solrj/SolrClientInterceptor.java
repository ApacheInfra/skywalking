/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.apache.skywalking.apm.plugin.solrj;

import org.apache.skywalking.apm.agent.core.context.ContextManager;
import org.apache.skywalking.apm.agent.core.context.trace.AbstractSpan;
import org.apache.skywalking.apm.agent.core.context.trace.SpanLayer;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import org.apache.skywalking.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.skywalking.apm.network.trace.component.ComponentsDefine;
import org.apache.skywalking.apm.plugin.solrj.commons.Context;
import org.apache.skywalking.apm.plugin.solrj.commons.SolrjInstance;
import org.apache.skywalking.apm.plugin.solrj.commons.SolrjTags;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.params.UpdateParams;
import org.apache.solr.common.util.NamedList;

import java.lang.reflect.Method;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SolrClientInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {
    private static final Pattern URL_REGEX = Pattern.compile("http(s)?://(?<domain>[\\w_.-]+(:\\d+)?)?/(?<path>solr/(?<collection>[\\w-_]+))?");

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        SolrjInstance instance = new SolrjInstance();
        HttpSolrClient client = (HttpSolrClient) objInst;

        Matcher matcher = URL_REGEX.matcher(client.getBaseURL());
        if (matcher.find()) {
            instance.setRemotePeer(matcher.group(2));
            if (matcher.group(4) != null) {
                instance.setCollection(matcher.group(4));
            }
        }
        objInst.setSkyWalkingDynamicField(instance);
    }

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        SolrRequest<?> request = (SolrRequest<?>) allArguments[0];
        SolrjInstance instance = (SolrjInstance) objInst.getSkyWalkingDynamicField();

        SolrParams params = getParams(request.getParams());
        String collection = getCollection(instance, allArguments[2]);

        AbstractSpan span = null;
        if ("/update".equals(request.getPath())) {
            AbstractUpdateRequest update = (AbstractUpdateRequest) request;

            String actionName = "ADD";
            AbstractUpdateRequest.ACTION action = update.getAction();
            if (action == null) {
                if (update instanceof UpdateRequest) {
                    UpdateRequest ur = (UpdateRequest) update;
                    List<SolrInputDocument> documents = ur.getDocuments();
                    if (documents == null) {
                        actionName = "DELETE";

                        span = getSpan(collection, request.getPath(), actionName, instance.getRemotePeer());
                        List<String> deleteBy = ur.getDeleteById();
                        if (deleteBy != null && !deleteBy.isEmpty()) {
                            span.tag(SolrjTags.TAG_DELETE_TYPE, "deleteByIds");
                            span.tag(SolrjTags.TAG_DELETE_VALUE, deleteBy.toString());
                        }
                        List<String> deleteQuery = ur.getDeleteQuery();
                        if (deleteQuery != null && !deleteQuery.isEmpty()) {
                            span.tag(SolrjTags.TAG_DELETE_TYPE, "deleteByQuery");
                            span.tag(SolrjTags.TAG_DELETE_VALUE, deleteQuery.toString());
                        }
                    } else {
                        span = getSpan(collection, request.getPath(), actionName, instance.getRemotePeer());
                        span.tag(SolrjTags.TAG_DOCS_SIZE, String.valueOf(documents.size()));
                        span.tag(SolrjTags.TAG_COMMIT_WITHIN, String.valueOf(update.getCommitWithin()));
                    }
                }
            } else {
                actionName = action.name();
                if (action == AbstractUpdateRequest.ACTION.COMMIT) {
                    span = getSpan(collection, request.getPath(), actionName, instance.getRemotePeer());

                    span.tag(SolrjTags.TAG_COMMIT, "true");
                    span.tag(SolrjTags.TAG_SOFT_COMMIT, params.get(UpdateParams.SOFT_COMMIT, ""));
                } else {
                    span = getSpan(collection, request.getPath(), actionName, instance.getRemotePeer());

                    span.tag(SolrjTags.TAG_OPTIMIZE, "true");
                    span.tag(SolrjTags.TAG_MAX_OPTIMIZE_SEGMENTS, params.get(UpdateParams.MAX_OPTIMIZE_SEGMENTS, "1"));
                }
            }
            span.tag(SolrjTags.TAG_ACTION, actionName);
        } else if (request instanceof QueryRequest) {
            String operatorName = String.format("solrJ/%s%s", collection, request.getPath());
            span = ContextManager.createExitSpan(operatorName, instance.getRemotePeer())
                    .setComponent(ComponentsDefine.SOLRJ)
                    .setLayer(SpanLayer.DB);
        }

        if (null == span)
            return;

        span.tag(SolrjTags.TAG_PATH, request.getPath());
        span.tag(SolrjTags.TAG_COLLECTION, collection);
        span.tag(SolrjTags.TAG_METHOD, request.getMethod().name());
        span.tag(SolrjTags.TAG_QT, params.get(CommonParams.QT, request.getPath()));

        Context.get().start();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (!ContextManager.isActive()) return ret;

        final Context context = Context.remove();
        final long elapse = System.currentTimeMillis() - context.getStartTime();

        AbstractSpan span = ContextManager.activeSpan();
        NamedList<Object> response = (NamedList<Object>) ret;
        if (response != null) {
            NamedList<Object> header = (NamedList<Object>) response.get("responseHeader");
            if (header != null) {
                span.tag(SolrjTags.TAG_STATUS, String.valueOf(header.get("status")));
                span.tag(SolrjTags.TAG_Q_TIME, String.valueOf(header.get("QTime")));
            }
            SolrDocumentList list = (SolrDocumentList) response.get("response");
            if (list != null) {
                span.tag(SolrjTags.TAG_NUM_FOUND, String.valueOf(list.getNumFound()));
            }
        }
        SolrjTags.addHttpResponse(span, context);
        SolrjTags.addElapseTime(span, elapse);

        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        if (ContextManager.isActive()) {
            ContextManager.activeSpan().errorOccurred().log(t);
        }
    }

    private static final AbstractSpan getSpan(String collection, String path, String action, String remotePeer) {
        String operatorName = String.format("solrJ/%s%s/%s", collection, path, action);
        return ContextManager.createExitSpan(operatorName, remotePeer)
                .setComponent(ComponentsDefine.SOLRJ)
                .setLayer(SpanLayer.DB);
    }

    private static final String getCollection(SolrjInstance instance, Object argument) {
        if (null == argument) {
            return instance.getCollection();
        }
        return String.valueOf(argument);
    }

    private static final SolrParams getParams(SolrParams params) {
        if (params == null) {
            return new ModifiableSolrParams();
        }
        return params;
    }
}
