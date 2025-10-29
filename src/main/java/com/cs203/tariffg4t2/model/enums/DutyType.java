package com.CS203.tariffg4t2.model.enums;

public enum DutyType {
    AD_VALOREM,          // % of customs value
    SPECIFIC_PER_HEAD,   // amount per head
    SPECIFIC_PER_KG,     // amount per kg
    COMPOUND,            // % + specific
    MIXED_MAX,           // max(% of value, specific)
    MIXED_MIN            // min(% of value, specific)
}

