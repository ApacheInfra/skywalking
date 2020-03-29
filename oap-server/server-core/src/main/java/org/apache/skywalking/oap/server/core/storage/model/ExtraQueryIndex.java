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

package org.apache.skywalking.oap.server.core.storage.model;

import lombok.Getter;
import org.apache.skywalking.oap.server.library.util.CollectionUtils;

/**
 * The extra query index if the storage could support this mode. Many NO-SQL support one column index only, in that
 * case, this could be ignored in the implementation level.
 */
@Getter
public class ExtraQueryIndex {
    private String indexName;
    private String[] columns;

    public ExtraQueryIndex(final String indexName, String mainColumn, final String[] withColumns) {
        this.indexName = indexName;
        if (CollectionUtils.isNotEmpty(withColumns)) {
            this.columns = new String[withColumns.length + 1];
            for (int i = 0; i < columns.length; i++) {
                if (i == 0) {
                    columns[i] = mainColumn;
                } else {
                    columns[i] = withColumns[i - 1];
                }
            }
        } else {
            this.columns = new String[] {mainColumn};
        }

    }

    /**
     * Keep the same name replacement as {@link ColumnName#overrideName(String, String)}
     *
     * @param oldName to be replaced.
     * @param newName to use in the storage level.
     */
    public void overrideName(String oldName, String newName) {
        for (int i = 0; i < columns.length; i++) {
            if (columns[i].equals(oldName)) {
                columns[i] = newName;
            }
        }
    }
}
