/*
 * Copyright (c) 2008-2019, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.row;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

import java.io.IOException;

/**
 * A row which joins two other rows.
 */
public class JoinRow implements Row, DataSerializable {
    /** Left row. */
    private Row left;

    /** Right row. */
    private Row right;

    public JoinRow() {
        // No-op.
    }

    public JoinRow(Row left, Row right) {
        this.left = left;
        this.right = right;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getColumn(int idx) {
        int leftColumnCount = left.getColumnCount();

        if (idx < leftColumnCount) {
            return (T) left.getColumn(idx);
        } else {
            return (T) right.getColumn(idx - leftColumnCount);
        }
    }

    @Override
    public int getColumnCount() {
        return left.getColumnCount() + right.getColumnCount();
    }

    @Override
    public void writeData(ObjectDataOutput out) throws IOException {
        out.writeObject(left);
        out.writeObject(right);
    }

    @Override
    public void readData(ObjectDataInput in) throws IOException {
        left = in.readObject();
        right = in.readObject();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("JoinRow{");

        for (int i = 0; i < getColumnCount(); i++) {
            if (i != 0) {
                res.append(", ");
            }

            Object column = getColumn(i);

            res.append(column != null ? column.toString() : "null");
        }

        res.append("}");

        return res.toString();
    }
}