package com.ecommerce.project.payout;

// Signs as stored in seller_ledger_entries.amount_minor_units.
public enum LedgerEntryType {
    SALE,                // + item price x quantity, when the order is paid
    COMMISSION,          // - marketplace commission on that sale
    REFUND,              // - the sale amount again, when the item is cancelled/returned
    COMMISSION_REVERSAL  // + the commission given back on a refunded item
}
