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

import quaero.components.filter.IFilter;

public class QueryJoinObject {

    private String mainTableName;
    private String joinTableName;
    private String joinTableAlias;
    private QuerySelectObject[] joinSelects;
    private QuerySelectObject[] joinSummatorySelects;
    private QueryJoinParamsObject[] joinParamTuples;
    private IFilter joinFilter;

    public String getJoinTableName() {
        return joinTableName;
    }

    public void setJoinTableName(final String joinTableName) {
        this.joinTableName = joinTableName;
    }

    public String getMainTableName() {
        return mainTableName;
    }

    public void setMainTableName(final String mainTableName) {
        this.mainTableName = mainTableName;
    }

    public QuerySelectObject[] getJoinSelects() {
        return joinSelects;
    }

    public void setJoinSelects(final QuerySelectObject[] joinSelects) {
        this.joinSelects = joinSelects;
    }

    public QueryJoinParamsObject[] getJoinParamTuples() {
        return joinParamTuples;
    }

    public void setJoinParamTuples(final QueryJoinParamsObject[] joinParamTuples) {
        this.joinParamTuples = joinParamTuples;
    }

    public IFilter getJoinFilter() {
        return joinFilter;
    }

    public void setJoinFilter(final IFilter joinFilter) {
        this.joinFilter = joinFilter;
    }

    public String getJoinTableAlias() {
        return joinTableAlias;
    }

    public void setJoinTableAlias(final String joinTableAlias) {
        this.joinTableAlias = joinTableAlias;
    }

    public QuerySelectObject[] getJoinSummatorySelects() {
        return joinSummatorySelects;
    }

    public void setJoinSummatorySelects(final QuerySelectObject[] joinSummatorySelects) {
        this.joinSummatorySelects = joinSummatorySelects;
    }

}
