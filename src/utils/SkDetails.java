/* Copyright (c) 2012 Google Inc.
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

package ir.miladesign.utils;

import org.json.JSONException;
import org.json.JSONObject;

public class SkDetails {
  String mItemType;
  String mSku;
  String mType;
  String mPrice;
  String mTitle;
  String mDescription;
  String mJson;

  public SkDetails(String jsonSkuDetails)
    throws JSONException
  {
    this("inapp", jsonSkuDetails);
  }

  public SkDetails(String itemType, String jsonSkuDetails)
    throws JSONException
  {
    this.mItemType = itemType;
    this.mJson = jsonSkuDetails;
    JSONObject o = new JSONObject(this.mJson);
    this.mSku = o.optString("productId");
    this.mType = o.optString("type");
    this.mPrice = o.optString("price");
    this.mTitle = o.optString("title");
    this.mDescription = o.optString("description");
  }

  public String getSku()
  {
    return this.mSku;
  }

  public String getType()
  {
    return this.mType;
  }

  public String getPrice()
  {
    return this.mPrice;
  }

  public String getTitle()
  {
    return this.mTitle;
  }

  public String getDescription()
  {
    return this.mDescription;
  }

  public String toString()
  {
    return "SkuDetails:" + this.mJson;
  }
}