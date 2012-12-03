/*
 * Copyright 2010-2012, CloudBees Inc.
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

package com.cloudbees.api;

public interface UploadProgress {
    /**
     * @param deltaCount   the number of new bytes sent since last notification.
     * @param totalWritten the total number of bytes sent so far
     * @param totalToSend  the total bytes being sent
     * @return
     */
    public void handleBytesWritten(long deltaCount, long totalWritten, long totalToSend);
}