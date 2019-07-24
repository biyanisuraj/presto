/*
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
package io.prestosql.plugin.druid.column;

import io.prestosql.spi.block.Block;
import io.prestosql.spi.block.BlockBuilder;
import io.prestosql.spi.type.Type;
import org.apache.druid.segment.ColumnValueSelector;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;
import static io.prestosql.spi.type.RealType.REAL;
import static java.lang.Float.floatToRawIntBits;
import static java.util.Objects.requireNonNull;

public class FloatColumnReader
        implements ColumnReader
{
    private final ColumnValueSelector<Float> valueSelector;

    public FloatColumnReader(ColumnValueSelector valueSelector)
    {
        this.valueSelector = requireNonNull(valueSelector, "value selector is null");
    }

    @Override
    public Block readBlock(Type type, int batchSize)
            throws IOException
    {
        checkArgument(type == REAL);
        BlockBuilder builder = type.createBlockBuilder(null, batchSize);
        for (int i = 0; i < batchSize; ++i) {
            type.writeLong(builder, floatToRawIntBits(valueSelector.getFloat()));
        }

        return builder.build();
    }
}
