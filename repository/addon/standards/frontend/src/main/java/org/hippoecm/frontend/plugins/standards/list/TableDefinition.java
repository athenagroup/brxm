/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.frontend.plugins.standards.list;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableDefinition {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private List<ListColumn> columns;
    private Map<String, Comparator> comparators;
    private boolean showColumnHeaders;
     
    public TableDefinition(List<ListColumn> columnList) {
        this(columnList, true);
    }
    
    public TableDefinition(List<ListColumn> columnList, boolean showColumnHeaders) {
        columns = new ArrayList<ListColumn>();
        comparators = new HashMap<String, Comparator>();
        for (ListColumn column: columnList) {
            columns.add(column);
            comparators.put(column.getSortProperty(), column.getComparator());
        }
        this.showColumnHeaders = showColumnHeaders;
    }
    
    public Map<String, Comparator> getComparators() {
        return comparators;
    }

    public ListColumn[] getColumns() {
        return (columns.toArray(new ListColumn[columns.size()]));
    }
        
    public boolean showColumnHeaders() {
        return showColumnHeaders;
    }

}
