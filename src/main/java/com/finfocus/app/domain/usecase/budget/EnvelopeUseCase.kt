package com.finfocus.app.domain.usecase.budget

import com.finfocus.app.domain.model.Budget
import javax.inject.Inject

class EnvelopeUseCase @Inject constructor() {
    fun warnIfExceeded(envelope: Budget): Boolean = envelope.spent > envelope.limit
}
