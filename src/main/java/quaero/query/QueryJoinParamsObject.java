/*
 * Copyright 2026 Ddementhius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package quaero.query;

import quaero.components.select.ISelect;

public class QueryJoinParamsObject {

    private String mainTableName;
    private ISelect mainParam;
    private ISelect joinParam;

    public ISelect getMainParam() {
        return mainParam;
    }

    public void setMainParam(final ISelect mainParam) {
        this.mainParam = mainParam;
    }

    public ISelect getJoinParam() {
        return joinParam;
    }

    public void setJoinParam(final ISelect joinParam) {
        this.joinParam = joinParam;
    }

    public String getMainTableName() {
        return mainTableName;
    }

    public void setMainTableName(final String mainTableName) {
        this.mainTableName = mainTableName;
    }

}
