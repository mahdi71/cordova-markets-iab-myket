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

import java.util.ArrayList;
import ir.miladesign.MdMarketsIAB;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Inventory {
    public Map < String, SkDetails > mSkuMap = new HashMap < String, SkDetails > ();
    public Map < String, MdMarketsIAB.Prchase > mPurchaseMap = new HashMap < String, MdMarketsIAB.Prchase > ();

    public SkDetails getSkuDetails(String sku) {
        return (SkDetails) this.mSkuMap.get(sku);
    }

    public MdMarketsIAB.Prchase getPurchase(String sku) {
        return (MdMarketsIAB.Prchase) this.mPurchaseMap.get(sku);
    }

    public boolean hasPurchase(String sku) {
        return this.mPurchaseMap.containsKey(sku);
    }

    public boolean hasDetails(String sku) {
        return this.mSkuMap.containsKey(sku);
    }

    public void erasePurchase(String sku) {
        if (this.mPurchaseMap.containsKey(sku))
            this.mPurchaseMap.remove(sku);
    }

    List < String > getAllOwnedSkus() {
        return new ArrayList < String > (this.mPurchaseMap.keySet());
    }

    List < String > getAllOwnedSkus(String itemType) {
        List < String > result = new ArrayList < String > ();
        for (MdMarketsIAB.Prchase p: this.mPurchaseMap.values()) {
            if (p.getItemType().equals(itemType)) {
                result.add(p.getProductId());
            }
        }
        return result;
    }

    public List < MdMarketsIAB.Prchase > getAllPurchases() {
        return new ArrayList < MdMarketsIAB.Prchase > (this.mPurchaseMap.values());
    }

    public void addSkuDetails(SkDetails d) {
        this.mSkuMap.put(d.getSku(), d);
    }

    public void addPurchase(MdMarketsIAB.Prchase p) {
        this.mPurchaseMap.put(p.getProductId(), p);
    }
}