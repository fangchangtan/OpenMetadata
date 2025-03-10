/* eslint-disable @typescript-eslint/no-explicit-any */
/*
 *  Copyright 2021 Collate
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * This schema defines the test ColumnValueLengthsToBeBetween. Test the value lengths in a
 * column to be between minimum and maximum value.
 */
export interface ColumnValuesLengthsToBeBetween {
  /**
   * The {maxLength} for the column length. if maxLength is not included, minLength is treated
   * as lowerBound and there will eb no maximum number of rows
   */
  maxLength?: number;
  /**
   * The {minLength} for the column length. If minLength is not included, maxLength is treated
   * as upperBound and there will be no minimum number of rows
   */
  minLength?: number;
}
