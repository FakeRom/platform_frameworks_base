/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define DEBUG false  // STOPSHIP if true
#include "Log.h"

#include "StatsPullerManager.h"
#include "atoms_info.h"
#include "puller_util.h"

namespace android {
namespace os {
namespace statsd {

using std::list;
using std::map;
using std::set;
using std::shared_ptr;
using std::sort;
using std::vector;

/**
 * Process all data and merge isolated with host if necessary.
 * For example:
 *   NetworkBytesAtom {
 *       int uid = 1;
 *       State process_state = 2;
 *       int byte_send = 3;
 *       int byte_recv = 4;
 *   }
 *   additive fields are {3, 4}
 * If we pulled the following events (uid1_child is an isolated uid which maps to uid1):
 * [uid1, fg, 100, 200]
 * [uid1_child, fg, 100, 200]
 * [uid1, bg, 100, 200]
 *
 * We want to merge them and results should be:
 * [uid1, fg, 200, 400]
 * [uid1, bg, 100, 200]
 *
 * All atoms should be of the same tagId. All fields should be present.
 */
void mapAndMergeIsolatedUidsToHostUid(vector<shared_ptr<LogEvent>>& data, const sp<UidMap>& uidMap,
                                      int tagId) {
    if (StatsPullerManager::kAllPullAtomInfo.find(tagId) ==
        StatsPullerManager::kAllPullAtomInfo.end()) {
        VLOG("Unknown pull atom id %d", tagId);
        return;
    }
    if ((android::util::AtomsInfo::kAtomsWithAttributionChain.find(tagId) ==
         android::util::AtomsInfo::kAtomsWithAttributionChain.end()) &&
        (android::util::AtomsInfo::kAtomsWithUidField.find(tagId) ==
         android::util::AtomsInfo::kAtomsWithUidField.end())) {
        VLOG("No uid or attribution chain to merge, atom %d", tagId);
        return;
    }

    // 1. Map all isolated uid in-place to host uid
    for (shared_ptr<LogEvent>& event : data) {
        if (event->GetTagId() != tagId) {
            ALOGE("Wrong atom. Expecting %d, got %d", tagId, event->GetTagId());
            return;
        }
        if (android::util::AtomsInfo::kAtomsWithAttributionChain.find(tagId) !=
            android::util::AtomsInfo::kAtomsWithAttributionChain.end()) {
            for (auto& value : *(event->getMutableValues())) {
                if (value.mField.getPosAtDepth(0) > kAttributionField) {
                    break;
                }
                if (isAttributionUidField(value)) {
                    const int hostUid = uidMap->getHostUidOrSelf(value.mValue.int_value);
                    value.mValue.setInt(hostUid);
                }
            }
        } else {
            auto it = android::util::AtomsInfo::kAtomsWithUidField.find(tagId);
            if (it != android::util::AtomsInfo::kAtomsWithUidField.end()) {
                int uidField = it->second;  // uidField is the field number in proto,
                // starting from 1
                if (uidField > 0 && (int)event->getValues().size() >= uidField &&
                    (event->getValues())[uidField - 1].mValue.getType() == INT) {
                    Value& value = (*event->getMutableValues())[uidField - 1].mValue;
                    const int hostUid = uidMap->getHostUidOrSelf(value.int_value);
                    value.setInt(hostUid);
                } else {
                    ALOGE("Malformed log, uid not found. %s", event->ToString().c_str());
                    return;
                }
            }
        }
    }

    // 2. sort the data, bit-wise
    sort(data.begin(), data.end(),
         [](const shared_ptr<LogEvent>& lhs, const shared_ptr<LogEvent>& rhs) {
             if (lhs->size() != rhs->size()) {
                 return lhs->size() < rhs->size();
             }
             const std::vector<FieldValue>& lhsValues = lhs->getValues();
             const std::vector<FieldValue>& rhsValues = rhs->getValues();
             for (int i = 0; i < (int)lhs->size(); i++) {
                 if (lhsValues[i] != rhsValues[i]) {
                     return lhsValues[i] < rhsValues[i];
                 }
             }
             return false;
         });

    vector<shared_ptr<LogEvent>> mergedData;
    const vector<int>& additiveFieldsVec =
            StatsPullerManager::kAllPullAtomInfo.find(tagId)->second.additiveFields;
    const set<int> additiveFields(additiveFieldsVec.begin(), additiveFieldsVec.end());
    bool needMerge = true;

    // 3. do the merge.
    // The loop invariant is this: for every event, check if it differs on
    // non-additive fields, or have different attribution chain length.
    // If so, no need to merge, add itself to the result.
    // Otherwise, merge the value onto the one immediately next to it.
    for (int i = 0; i < (int)data.size() - 1; i++) {
        // Size different, must be different chains.
        if (data[i]->size() != data[i + 1]->size()) {
            mergedData.push_back(data[i]);
            continue;
        }
        vector<FieldValue>* lhsValues = data[i]->getMutableValues();
        vector<FieldValue>* rhsValues = data[i + 1]->getMutableValues();
        needMerge = true;
        for (int p = 0; p < (int)lhsValues->size(); p++) {
            if ((*lhsValues)[p] != (*rhsValues)[p]) {
                int pos = (*lhsValues)[p].mField.getPosAtDepth(0);
                // Differ on non-additive field, abort.
                if (additiveFields.find(pos) == additiveFields.end()) {
                    needMerge = false;
                    break;
                }
            }
        }
        if (!needMerge) {
            mergedData.push_back(data[i]);
            continue;
        }
        // This should be infrequent operation.
        for (int p = 0; p < (int)lhsValues->size(); p++) {
            int pos = (*lhsValues)[p].mField.getPosAtDepth(0);
            if (additiveFields.find(pos) != additiveFields.end()) {
                (*rhsValues)[p].mValue += (*lhsValues)[p].mValue;
            }
        }
    }
    mergedData.push_back(data.back());

    data.clear();
    data = mergedData;
}

}  // namespace statsd
}  // namespace os
}  // namespace android
