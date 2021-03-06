/*
  * Copyright 2018 Ryan Ward
  *
  *    Licensed under the Apache License, Version 2.0 (the "License");
  *    you may not use this file except in compliance with the License.
  *    You may obtain a copy of the License at
  *
  *        http://www.apache.org/licenses/LICENSE-2.0
  *
  *    Unless required by applicable law or agreed to in writing, software
  *    distributed under the License is distributed on an "AS IS" BASIS,
  *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  *    See the License for the specific language governing permissions and
  *    limitations under the License.
  */

package net.frostedbytes.android.trendfeeder.views;

import android.content.Context;
import android.util.AttributeSet;

public class TouchableImageView extends android.support.v7.widget.AppCompatImageView {

  public TouchableImageView(Context context) {
    super(context);
  }

  public TouchableImageView(Context context, AttributeSet attrs) {
    super(context, attrs);

  }

  @Override
  public boolean performClick() {
    super.performClick();

    return true;
  }
}
