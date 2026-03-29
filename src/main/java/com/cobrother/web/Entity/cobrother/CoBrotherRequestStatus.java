package com.cobrother.web.Entity.cobrother;

public enum CoBrotherRequestStatus {
    PENDING,            // addon paid, waiting for admin to assign
    PAYMENT_PENDING,      // admin forwarded, waiting for lister to pay ₹1000
    PAYMENT_COMPLETED,    // lister paid, forwarded to cobrother
    FORWARDED,            // cobrother notified
    ACCEPTED,             // cobrother accepted
    REJECTED,             // cobrother rejected
    CANCELLED             // lister cancelled payment
}
