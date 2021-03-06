/*
 * Copyright 2021 Plume Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.plume.oss.util

import io.shiftleft.codepropertygraph.generated.PropertyNames.*

object ExtractorConst {
    const val LANGUAGE_FRONTEND = "Plume"
    val plumeVersion: String by lazy { javaClass.`package`.implementationVersion ?: "X.X.X" }
    const val ENTRYPOINT = "BODY"
    const val UNKNOWN = "<unknown>"
    const val GLOBAL = "<global>"
    val BOOLEAN_TYPES = setOf(
        IS_METHOD_NEVER_OVERRIDDEN,
        IS_EXTERNAL
    )
    val INT_TYPES = setOf(
        COLUMN_NUMBER,
        ARGUMENT_INDEX,
        ORDER,
        LINE_NUMBER,
        LINE_NUMBER_END,
        COLUMN_NUMBER_END
    )
}