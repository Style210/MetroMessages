package com.metromessages.data.model.facebook

@JvmInline
value class PhoneNumberId(val value: String) {
    companion object {
        fun fromPhoneNumber(phoneNumber: String): PhoneNumberId {
            val normalized = phoneNumber.replace(Regex("[^+0-9]"), "").trim()
            return PhoneNumberId("pn_$normalized")
        }

        fun fromConversationId(conversationId: String): PhoneNumberId? {
            return if (conversationId.startsWith("conv_")) {
                PhoneNumberId(conversationId.removePrefix("conv_"))
            } else {
                null
            }
        }

        fun fromContactId(contactId: Long): PhoneNumberId {
            return PhoneNumberId("contact_$contactId")
        }

        fun fromSmsConversationId(smsId: String): PhoneNumberId {
            return if (smsId.startsWith("sms_")) {
                PhoneNumberId(smsId.replace("sms_", "pn_"))
            } else {
                PhoneNumberId("pn_$smsId")
            }
        }
    }

    fun toConversationId(): String = "conv_$value"

    fun toContactId(): Long? {
        return if (value.startsWith("contact_")) {
            value.removePrefix("contact_").toLongOrNull()
        } else {
            null
        }
    }

    fun toRawPhoneNumber(): String? {
        return if (value.startsWith("pn_")) {
            value.removePrefix("pn_")
        } else {
            null
        }
    }

    suspend fun toContactId(facebookDao: FacebookDao): Long? {
        val conversation = facebookDao.getConversationByPhoneNumberId(value)
        return conversation?.linkedContactId
    }

    override fun toString(): String = value
}