/*
 * Copyright 2020 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.uiexport.exporter.json;

import com.google.gson.*;
import io.jmix.core.JmixEntity;
import io.jmix.core.Messages;
import io.jmix.core.metamodel.model.MetaPropertyPath;
import io.jmix.ui.component.Table;
import io.jmix.ui.download.ByteArrayDataProvider;
import io.jmix.ui.download.DownloadFormat;
import io.jmix.ui.download.Downloader;
import io.jmix.uiexport.exporter.AbstractTableExporter;
import io.jmix.uiexport.exporter.ExportMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.function.Function;

@Component(JsonExporter.NAME)
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class JsonExporter extends AbstractTableExporter<JsonExporter> {

    public static final String NAME = "export_JsonExporter";

    @Autowired
    protected Messages messages;

    protected Function<GsonBuilder, GsonBuilder> gsonConfigurer;

    /**
     * Configure Gson builder for export
     *
     * @param gsonConfigurator Gson configurator function
     * @return exporter instance
     */
    public JsonExporter withGsonConfigurator(Function<GsonBuilder, GsonBuilder> gsonConfigurator) {
        this.gsonConfigurer = gsonConfigurator;
        return this;
    }

    @Override
    public void download(Downloader downloader, Table<JmixEntity> table, ExportMode exportMode) {
        Collection<JmixEntity> items = getItems(table, exportMode);
        Gson gson = createGsonForSerialization();
        JsonArray jsonElements = new JsonArray();
        for (JmixEntity entity : items) {
            JsonObject jsonObject = new JsonObject();
            for (Table.Column<JmixEntity> column : table.getColumns()) {
                if (column.getId() instanceof MetaPropertyPath) {
                    MetaPropertyPath propertyPath = (MetaPropertyPath) column.getId();
                    Object columnValue = getColumnValue(table, column, entity);
                    if (columnValue != null) {
                        jsonObject.add(propertyPath.getMetaProperty().getName(),
                                new JsonPrimitive(formatColumnValue(columnValue, column)));
                    } else {
                        jsonObject.add(propertyPath.getMetaProperty().getName(),
                                JsonNull.INSTANCE);
                    }
                }
            }
            jsonElements.add(jsonObject);
        }
        downloader.download(new ByteArrayDataProvider(gson.toJson(jsonElements).getBytes(),
                        uiProperties.getSaveExportedByteArrayDataThresholdBytes(), coreProperties.getTempDir()),
                getFileName(table) + ".json", DownloadFormat.JSON);
    }

    protected Gson createGsonForSerialization() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        if (gsonConfigurer != null) {
            gsonConfigurer.apply(gsonBuilder);
        }
        return gsonBuilder.create();
    }

    protected Collection<JmixEntity> getItems(Table<JmixEntity> table, ExportMode exportMode) {
        return ExportMode.ALL == exportMode ? table.getItems().getItems() : table.getSelected();
    }

    @Override
    public String getCaption() {
        return messages.getMessage("jsonExporter.caption");
    }
}
