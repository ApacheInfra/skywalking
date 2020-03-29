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
 */

package org.apache.skywalking.oap.server.core.storage.model;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.oap.server.core.source.DefaultScopeDefine;
import org.apache.skywalking.oap.server.core.storage.annotation.Column;
import org.apache.skywalking.oap.server.core.storage.annotation.MultipleQueryUnifiedIndex;
import org.apache.skywalking.oap.server.core.storage.annotation.QueryUnifiedIndex;
import org.apache.skywalking.oap.server.core.storage.annotation.Storage;
import org.apache.skywalking.oap.server.core.storage.annotation.ValueColumnMetadata;

/**
 * StorageModels manages all models detected by the core.
 */
@Slf4j
public class StorageModels implements IModelManager, INewModel, IModelOverride {
    private final List<Model> models;

    public StorageModels() {
        this.models = new LinkedList<>();
    }

    @Override
    public Model add(Class aClass, int scopeId, Storage storage, boolean record) {
        // Check this scope id is valid.
        DefaultScopeDefine.nameOf(scopeId);

        for (Model model : models) {
            if (model.getName().equals(storage.getModelName())) {
                return model;
            }
        }

        List<ModelColumn> modelColumns = new ArrayList<>();
        List<ExtraQueryIndex> extraQueryIndices = new ArrayList<>();
        retrieval(aClass, storage.getModelName(), modelColumns, extraQueryIndices);

        Model model = new Model(
            storage.getModelName(), modelColumns, extraQueryIndices, storage.isCapableOfTimeSeries(),
            storage.isDeleteHistory(), scopeId,
            storage.getDownsampling(), record
        );
        models.add(model);

        return model;
    }

    private void retrieval(Class clazz,
                           String modelName,
                           List<ModelColumn> modelColumns,
                           List<ExtraQueryIndex> extraQueryIndices) {
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                Column column = field.getAnnotation(Column.class);
                modelColumns.add(
                    new ModelColumn(
                        new ColumnName(modelName, column.columnName()), field.getType(), column.matchQuery(), column
                        .storageOnly(), column.isValue(), column.length()));
                if (log.isDebugEnabled()) {
                    log.debug("The field named {} with the {} type", column.columnName(), field.getType());
                }
                if (column.isValue()) {
                    ValueColumnMetadata.INSTANCE.putIfAbsent(modelName, column.columnName(), column.function());
                }

                List<QueryUnifiedIndex> indexDefinitions = new ArrayList<>();
                if (field.isAnnotationPresent(QueryUnifiedIndex.class)) {
                    indexDefinitions.add(field.getAnnotation(QueryUnifiedIndex.class));
                }

                if (field.isAnnotationPresent(MultipleQueryUnifiedIndex.class)) {
                    for (final QueryUnifiedIndex queryUnifiedIndex : field.getAnnotation(
                        MultipleQueryUnifiedIndex.class)
                                                                          .value()) {
                        indexDefinitions.add(queryUnifiedIndex);
                    }
                }

                indexDefinitions.forEach(indexDefinition -> {
                    extraQueryIndices.add(new ExtraQueryIndex(
                        indexDefinition.name() + "_UIDX",
                        column.columnName(),
                        indexDefinition.withColumns()
                    ));
                });
            }
        }

        if (Objects.nonNull(clazz.getSuperclass())) {
            retrieval(clazz.getSuperclass(), modelName, modelColumns, extraQueryIndices);
        }
    }

    @Override
    public void overrideColumnName(String columnName, String newName) {
        models.forEach(model -> {
            model.getColumns().forEach(column -> column.getColumnName().overrideName(columnName, newName));
            model.getExtraQueryIndices().forEach(extraQueryIndex -> extraQueryIndex.overrideName(columnName, newName));
        });
    }

    @Override
    public List<Model> allModels() {
        return models;
    }
}
