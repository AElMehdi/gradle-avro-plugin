/**
 * Copyright © 2015 Commerce Technologies, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.commercehub.gradle.plugin.avro;

import org.apache.avro.Schema;
import org.gradle.api.Project;

import java.io.File;
import java.util.*;

class ProcessingState {
    private final Map<String, TypeState> typeStates = new HashMap<String, TypeState>();
    private final Set<FileState> delayedFiles = new LinkedHashSet<FileState>();
    private final Queue<FileState> filesToProcess = new LinkedList<FileState>();
    private int processedTotal = 0;

    ProcessingState(Set<File> files, Project project) {
        for (File file : files) {
            filesToProcess.add(new FileState(file, project.relativePath(file)));
        }
    }

    Map<String, Schema> determineParserTypes(FileState fileState) {
        Set<String> duplicateTypeNames = fileState.getDuplicateTypeNames();
        Map<String, Schema> types = new HashMap<String, Schema>();
        for (TypeState typeState : typeStates.values()) {
            String typeName = typeState.getName();
            if (!duplicateTypeNames.contains(typeName)) {
                types.put(typeState.getName(), typeState.getSchema());
            }
        }
        return types;
    }

    void processTypeDefinitions(FileState fileState, Map<String, Schema> newTypes) {
        String path = fileState.getPath();
        for (Map.Entry<String, Schema> entry : newTypes.entrySet()) {
            String typeName = entry.getKey();
            Schema schema = entry.getValue();
            getTypeState(typeName).processTypeDefinition(path, schema);
        }
        fileState.clearError();
        processedTotal++;
        queueDelayedFilesForProcessing();
    }

    Set<FileState> getFailedFiles() {
        return delayedFiles;
    }

    TypeState getTypeState(String typeName) {
        TypeState typeState = typeStates.get(typeName);
        if (typeState == null) {
            typeState = new TypeState(typeName);
            typeStates.put(typeName, typeState);
        }
        return typeState;
    }

    void queueForProcessing(FileState fileState) {
        filesToProcess.add(fileState);
    }

    void queueForDelayedProcessing(FileState fileState) {
        delayedFiles.add(fileState);
    }

    private void queueDelayedFilesForProcessing() {
        filesToProcess.addAll(delayedFiles);
        delayedFiles.clear();
    }

    FileState nextFileState() {
        return filesToProcess.poll();
    }

    boolean isWorkRemaining() {
        return !filesToProcess.isEmpty();
    }

    int getProcessedTotal() {
        return processedTotal;
    }
}
